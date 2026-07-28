package com.mrfuzzihead.fluiddrawers.client.tesr;

import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.opengl.GL11;

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
 * Deferred to Phase 14: the {@code isShowingQuantity()} floating label + its sync packets.
 */
@SideOnly(Side.CLIENT)
public class RenderTileTank extends TileEntitySpecialRenderer {

    // Half-width of the inner fluid box (1.12.2 `d = 0.375`). Centered on 0.5 -> x/z 0.125..0.875.
    private static final double HALF_WIDTH = 0.375;
    // Y offset so the fluid sits just above the bottom metal slab (2/16 = 0.125).
    private static final double Y_OFFSET = 0.125;
    // Fluid occupies up to 0.75 of the block height (12/16), leaving room for the top slab.
    private static final double HEIGHT_SCALE = 0.75;

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        TileTank tile = (TileTank) te;
        if (tile.getWorldObj() == null) return;
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

        // Packed sky/block brightness at the tank, raised by the fluid's luminosity (the 1.7.10
        // equivalent of the 1.12.2 func_178459_a().func_175626_b(pos, luminosity)). Packed as
        // (skyLight << 20) | (blockLight << 4), so ambLight % 65536 == blockLight<<4 and
        // ambLight / 65536 == skyLight<<4.
        int ambLight = tile.getWorldObj()
            .getLightBrightnessForSkyBlocks(
                tile.xCoord,
                tile.yCoord,
                tile.zCoord,
                fluid.getFluid()
                    .getLuminosity(fluid));

        // Fluid still icons live on the block atlas (Fluid.getSpriteNumber() == 0).
        bindTexture(TextureMap.locationBlocksTexture);

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

        // TODO Phase 14: if (tile.getAttributes().isShowingQuantity()) render the floating
        // "fluid name / X mB" label on all four sides (FontRenderer + the
        // SPacketSyncFluidDrawerCount/Fluid custom sync packets). Deferred -- not part of Phase 7.
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

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }
}
