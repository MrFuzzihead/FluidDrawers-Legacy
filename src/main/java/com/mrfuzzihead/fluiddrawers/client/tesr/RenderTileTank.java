package com.mrfuzzihead.fluiddrawers.client.tesr;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.fluiddrawers.Config;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Fluid TESR for the basic tank. Ports the 1.12.2 {@code RenderTileTank} to a 1.7.10
 * {@link TileEntitySpecialRenderer}, drawing the stored fluid as a colored, level-scaled sprite
 * inside the hollow glass frame.
 *
 * <p>
 * Geometry (from the 1.12.2 source, authoritative): an inner box of half-width {@code 0.375}
 * centered on the block (x/z span {@code 0.125..0.875} after the {@code (0.5, 0.125, 0.5)}
 * translate), sitting above the bottom slab at {@code y = 0.125} and rising to
 * {@code y = 0.125 + 0.75 * fill}. Four side faces via 90&deg; Y rotations; the top surface only
 * when not full.
 *
 * <p>
 * Lighting is the classic 1.7.10 "fluid renders solid black" trap: TESR quads do not inherit
 * the chunk mesh's baked lighting. We sample
 * {@code world.getLightBrightnessForSkyBlocks(x, y, z, luminosity)} and apply it via
 * {@link OpenGlHelper#setLightmapTextureCoords} (SD pattern) and {@link Tessellator#setBrightness}
 * (per-vertex, {@code RenderBlockFluid} pattern) so the quads are guaranteed lit. Fixed-function
 * GL lighting is off for the draw (the lightmap on texture unit 1 still applies via the texture
 * env), so brightness tracks torchlight changes without double-darkening the tint.
 *
 * <p>
 * The fluid-draw core is factored into {@link #renderFluid} so the WAILA HUD item renderer
 * ({@link com.mrfuzzihead.fluiddrawers.client.renderer.ItemRendererTank}) can reuse the exact
 * geometry/tinting for the tank's item icon via {@link #renderFluidForItem}, using full-bright
 * lighting and a transient (world-less) {@link TileTank} reconstructed from portable NBT.
 *
 * <p>
 * Phase 14: when the {@code isShowingQuantity()} attribute is set (toggled by the SD quantify
 * key), {@link #renderQuantityLabel} draws a floating "fluid name / X mB" label on all four
 * sides, porting the 1.12.2 {@code renderLabelText} 4-side loop to 1.7.10 {@link FontRenderer}
 * + {@link GL11}. The label is hidden when the tank is concealed (matching the 1.12.2 nesting
 * inside the {@code !isConcealed()} guard).
 */
@SideOnly(Side.CLIENT)
public class RenderTileTank extends TileEntitySpecialRenderer {

    // Half-width of the inner fluid box (1.12.2 `d = 0.375`). Centered on 0.5 -> x/z 0.125..0.875.
    private static final double HALF_WIDTH = 0.375;
    // Y offset so the fluid sits just above the bottom metal slab (2/16 = 0.125).
    private static final double Y_OFFSET = 0.125;
    // Fluid occupies up to 0.75 of the block height (12/16), leaving room for the top slab.
    private static final double HEIGHT_SCALE = 0.75;

    // Packed full-bright brightness (skyLight=15, blockLight=15): (15 << 20) | (15 << 4). Used
    // for the item-icon path where there is no world to sample brightness from. Matches the value
    // Minecraft treats as "full bright" (also = the (240, 240) lightmap coord).
    private static final int FULL_BRIGHT = 0x00F000F0;

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        TileTank tile = (TileTank) te;
        if (tile.getWorldObj() == null) return;

        // Luminosity comes from the stored fluid (lava glows, etc.); 0 when the tank is empty.
        FluidDrawer drawer = tile.getDrawerGroup()
            .getFluidDrawer();
        FluidStack fluid = drawer.getStoredFluid();
        int luminosity = fluid != null ? fluid.getFluid()
            .getLuminosity(fluid) : 0;

        // Packed sky/block brightness at the tank, raised by the fluid's luminosity (the 1.7.10
        // equivalent of the 1.12.2 func_178459_a().func_175626_b(pos, luminosity)). Packed as
        // (skyLight << 20) | (blockLight << 4), so ambLight % 65536 == blockLight<<4 and
        // ambLight / 65536 == skyLight<<4.
        int ambLight = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(tile.xCoord, tile.yCoord, tile.zCoord, luminosity);

        // Fluid still icons live on the block atlas (Fluid.getSpriteNumber() == 0).
        bindTexture(TextureMap.locationBlocksTexture);

        renderFluid(tile, ambLight, x, y, z);

        // Phase 14: floating "fluid name / X mB" label on all four sides when the quantify
        // attribute is set. The fluid amount + attribute are already synced to the client via
        // the vanilla description packet, so no custom sync packets are needed (the 1.12.2
        // SPacketSyncFluidDrawerCount/Fluid packets worked around Chameleon's split TE data,
        // which our unified portable NBT does not have).
        renderQuantityLabel(tile, x, y, z);
    }

    /**
     * Draws the tank's stored fluid for an item icon (WAILA HUD). Uses full-bright lighting (no
     * world) and draws in block-local {@code 0..1} space; the caller wraps the call in a
     * {@code -0.5} translate so the model is centered, matching
     * {@link com.mrfuzzihead.fluiddrawers.client.renderer.BlockTankRenderer#renderInventoryBlock}.
     *
     * <p>
     * {@code tile} is a transient {@link TileTank} reconstructed from the item's portable NBT
     * (worldObj == null); this method touches only portable state, so it is null-world-safe.
     */
    public static void renderFluidForItem(TileTank tile) {
        // The TESR's instance bindTexture() is unavailable in a static context; bind the block
        // atlas directly via the shared texture manager (same texture the still icons live on).
        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(TextureMap.locationBlocksTexture);
        renderFluid(tile, FULL_BRIGHT, 0.0D, 0.0D, 0.0D);
    }

    /**
     * Core fluid draw: computes fill/alpha/tint from the tank's portable state, sets up the GL
     * state (lighting off, optional translucent blend, lightmap), translates to the centered
     * fluid box at {@code (x, y, z) + (0.5, Y_OFFSET, 0.5)}, draws the four side faces + the top
     * surface, and restores GL state. Caller is responsible for binding the block atlas before
     * calling. {@code ambLight} is the packed brightness to bake per-vertex.
     */
    private static void renderFluid(TileTank tile, int ambLight, double x, double y, double z) {
        // A concealed tank hides its contents (1.12.2 isConcealed() guard).
        if (tile.getAttributes()
            .isConcealed()) return;

        FluidDrawer drawer = tile.getDrawerGroup()
            .getFluidDrawer();
        FluidStack fluid = drawer.getStoredFluid();
        if (fluid == null || fluid.amount == 0) return;

        IIcon icon = fluid.getFluid()
            .getStillIcon();
        if (icon == null) return; // fluid icon not stitched yet (shouldn't happen at render time)

        int capacity = drawer.getMaxCapacity();
        boolean vending = tile.getAttributes()
            .isUnlimitedVending();

        // Fluid tint (1.12.2: fluid.getFluid().getColor(fluid) -> r/g/b byte components).
        int col = fluid.getFluid()
            .getColor(fluid);
        float r = (col >> 16 & 0xFF) / 255.0F;
        float g = (col >> 8 & 0xFF) / 255.0F;
        float b = (col & 0xFF) / 255.0F;

        // Fill height / alpha. 1.12.2 logic, with the one verified remap: 1.7.10 has no
        // Fluid.isLighterThanAir(); the Fluid javadoc states "negative density indicates that the
        // fluid is lighter than air", so density >= 0 is the normal-liquid case.
        // vending -> full height, opaque
        // normal liquid (not gaseous, density >= 0) -> height = fill ratio, opaque
        // gaseous / lighter-than-air -> full height, alpha = fill ratio
        float fillPercent;
        float alpha;
        if (vending) {
            fillPercent = 1.0F;
            alpha = 1.0F;
        } else if (!fluid.getFluid()
            .isGaseous()
            && fluid.getFluid()
                .getDensity() >= 0) {
                    fillPercent = clamp01((float) fluid.amount / (float) capacity);
                    if (fillPercent < 0.01F) fillPercent = 0.01F;
                    alpha = 1.0F;
                } else {
                    fillPercent = 1.0F;
                    alpha = clamp01((float) fluid.amount / (float) capacity);
                    if (alpha < 0.01F) alpha = 0.01F;
                }

        // --- GL state: minimal and symmetric. Blend is enabled ONLY for translucent fluids
        // (gaseous / lighter-than-air, alpha < 1.0). The common opaque case (water, lava) leaves
        // blend untouched, which eliminates a class of GL-state leak (a rare white flash observed
        // during fill when blend was left enabled for opaque quads). Fixed-function GL lighting is
        // disabled for the draw so the OpenGL light does not double-darken the tint; the lightmap
        // (texture unit 1) still applies via the texture env. The fluid box is convex, so default
        // back-face culling renders it correctly from every exterior angle -- GL_CULL_FACE is left
        // untouched (not disabling it removes another leak surface). ---
        boolean prevLighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
        float prevLX = OpenGlHelper.lastBrightnessX;
        float prevLY = OpenGlHelper.lastBrightnessY;
        boolean useBlend = alpha < 1.0F;

        GL11.glDisable(GL11.GL_LIGHTING);
        if (useBlend) {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        // DoD: apply the lightmap via OpenGlHelper.setLightmapTextureCoords (matches SD's
        // TileEntityDrawersRenderer). lu/lv = (blockLight<<4, skyLight<<4). Per-vertex
        // setBrightness(ambLight) below bakes the same coords into the quads as a guarantee.
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, ambLight % 65536, ambLight / 65536);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        GL11.glTranslated(0.5, Y_OFFSET, 0.5);

        Tessellator tess = Tessellator.instance;
        double yMax = fillPercent * HEIGHT_SCALE;

        // Four side faces: draw the +Z face, then rotate 90 degrees about Y three more times
        // (rotations accumulate: 0, 90, 180, 270) -- matches the 1.12.2 push/draw/rotate loop.
        GL11.glPushMatrix();
        drawFluidSide(tess, icon, yMax, fillPercent, r, g, b, alpha, ambLight);
        for (int i = 0; i < 3; i++) {
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            drawFluidSide(tess, icon, yMax, fillPercent, r, g, b, alpha, ambLight);
        }
        GL11.glPopMatrix();

        // Top surface: only when not full, so the fluid surface is visible through the glass.
        if (fillPercent < 1.0F) {
            drawFluidTop(tess, icon, yMax, r, g, b, alpha, ambLight);
        }

        GL11.glPopMatrix();

        // --- restore GL state so subsequent rendering is unaffected ---
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, prevLX, prevLY);
        if (prevLighting) GL11.glEnable(GL11.GL_LIGHTING);
        if (useBlend) GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * One side face of the fluid box: a quad at {@code z = +HALF_WIDTH}, x in
     * {@code [-HALF_WIDTH, HALF_WIDTH]}, y in {@code [0, yMax]}. UV samples the middle horizontal
     * band (icon columns 3..13) and the vertical band {@code [0, fillPercent*16]}. Ports the
     * 1.12.2 {@code drawFluidSide}.
     */
    private static void drawFluidSide(Tessellator tess, IIcon icon, double yMax, float fillPercent, float r, float g,
        float b, float alpha, int brightness) {
        double uMin = icon.getInterpolatedU(3.0D);
        double uMax = icon.getInterpolatedU(13.0D);
        double vMin = icon.getMinV();
        double vMax = icon.getInterpolatedV(fillPercent * 16.0D);

        tess.startDrawingQuads();
        // setBrightness bakes the lightmap into each vertex (the RenderBlockFluid per-vertex
        // path); setColorRGBA_F sets the fluid tint+alpha. Both apply to all four vertices below.
        tess.setBrightness(brightness);
        tess.setColorRGBA_F(r, g, b, alpha);
        tess.addVertexWithUV(-HALF_WIDTH, yMax, HALF_WIDTH, uMin, vMin);
        tess.addVertexWithUV(-HALF_WIDTH, 0.0D, HALF_WIDTH, uMin, vMax);
        tess.addVertexWithUV(HALF_WIDTH, 0.0D, HALF_WIDTH, uMax, vMax);
        tess.addVertexWithUV(HALF_WIDTH, yMax, HALF_WIDTH, uMax, vMin);
        tess.draw();
    }

    /**
     * The fluid top surface: a quad at {@code y = yMax}, x/z in
     * {@code [-HALF_WIDTH, HALF_WIDTH]}, using the full icon UV. Ports the 1.12.2 top-face quad
     * (drawn only when {@code fillPercent < 1.0}).
     */
    private static void drawFluidTop(Tessellator tess, IIcon icon, double yMax, float r, float g, float b, float alpha,
        int brightness) {
        double uMin = icon.getMinU();
        double uMax = icon.getMaxU();
        double vMin = icon.getMinV();
        double vMax = icon.getMaxV();

        tess.startDrawingQuads();
        tess.setBrightness(brightness);
        tess.setColorRGBA_F(r, g, b, alpha);
        tess.addVertexWithUV(-HALF_WIDTH, yMax, -HALF_WIDTH, uMin, vMin);
        tess.addVertexWithUV(-HALF_WIDTH, yMax, HALF_WIDTH, uMin, vMax);
        tess.addVertexWithUV(HALF_WIDTH, yMax, HALF_WIDTH, uMax, vMax);
        tess.addVertexWithUV(HALF_WIDTH, yMax, -HALF_WIDTH, uMax, vMin);
        tess.draw();
    }

    /**
     * Phase 14 floating label: draws "fluid name / X mB" on all four sides of the tank when the
     * {@code isShowingQuantity()} attribute is set. Ports the 1.12.2 {@code RenderTileTank}
     * label block verbatim (the GL transforms are geometry, not API): push, scale
     * {@code (1/128, -1/128, 1/128)}, rotate 180&deg; about Y (so the text faces outward and is
     * not mirrored), translate to the first face, then draw + rotate 90&deg; three more times.
     *
     * <p>
     * Gates (matching the 1.12.2 nesting): not concealed, fluid present with amount &gt; 0, and
     * {@code isShowingQuantity()}. GL lighting is disabled and depth-write masked so the text is
     * full-bright and not occluded by the glass frame (SD {@code renderText} pattern); both are
     * restored after.
     */
    private void renderQuantityLabel(TileTank tile, double x, double y, double z) {
        // A concealed tank hides its contents, including the quantity label (1.12.2 nests the
        // label inside the !isConcealed() guard).
        if (tile.getAttributes()
            .isConcealed()) return;
        if (!tile.getAttributes()
            .isShowingQuantity()) return;

        FluidDrawer drawer = tile.getDrawerGroup()
            .getFluidDrawer();
        FluidStack fluid = drawer.getStoredFluid();
        if (fluid == null || fluid.amount <= 0) return;

        FontRenderer fr = Minecraft.getMinecraft().fontRenderer;
        String nameLabel = fluid.getLocalizedName();
        String qtyLabel = String.format("%,d mB", fluid.amount);
        float nameHalfWidth = fr.getStringWidth(nameLabel) / 2.0F;
        float qtyHalfWidth = fr.getStringWidth(qtyLabel) / 2.0F;

        boolean prevLighting = GL11.glGetBoolean(GL11.GL_LIGHTING);
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);
        // Scale to font units (1 block = 128 units), flip Y (font Y is down, world Y is up).
        GL11.glScalef(0.0078125F, -0.0078125F, 0.0078125F);
        // Face outward on the first side (text is mirrored without the 180deg Y rotation).
        GL11.glRotatef(180.0F, 0.0F, 1.0F, 0.0F);
        GL11.glTranslatef(-64.0F, -8.0F - fr.FONT_HEIGHT / 2.0F, 0.01F);
        renderLabelText(fr, nameLabel, nameHalfWidth, qtyLabel, qtyHalfWidth);

        // Draw the remaining three sides: step to the next face center and rotate 90deg about Y.
        for (int i = 0; i < 3; i++) {
            GL11.glTranslatef(64.01F, 0.0F, -64.01F);
            GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F);
            renderLabelText(fr, nameLabel, nameHalfWidth, qtyLabel, qtyHalfWidth);
        }

        GL11.glPopMatrix();

        GL11.glDepthMask(prevDepthMask);
        if (prevLighting) GL11.glEnable(GL11.GL_LIGHTING);
    }

    /**
     * Draws the two-line label (fluid name above, quantity below) centered horizontally at the
     * current origin. The name line is gated by {@link Config#quantifyShowsFluidName}. Each line
     * is wrapped in its own push/translate/pop so they position independently. Ports the 1.12.2
     * {@code renderLabelText}; {@code drawString(text, 0, 0, -1)} is the no-shadow variant
     * (color {@code -1} = {@code 0xFFFFFFFF} = white, full alpha).
     */
    private static void renderLabelText(FontRenderer fr, String line1, float halfWidth1, String line2,
        float halfWidth2) {
        if (Config.quantifyShowsFluidName) {
            GL11.glPushMatrix();
            GL11.glTranslatef(-halfWidth1, -16.0F, 0.0F);
            fr.drawString(line1, 0, 0, -1);
            GL11.glPopMatrix();
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(-halfWidth2, 0.0F, 0.0F);
        fr.drawString(line2, 0, 0, -1);
        GL11.glPopMatrix();
    }

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }
}
