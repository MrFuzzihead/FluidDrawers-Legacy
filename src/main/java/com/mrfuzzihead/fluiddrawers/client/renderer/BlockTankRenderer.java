package com.mrfuzzihead.fluiddrawers.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/**
 * Replaces the 1.12.2 JSON block model (models/block/tank.json) with a 1.7.10
 * ISimpleBlockRenderingHandler. Draws the same 7 elements in 0..1 block space (1.12.2 pixel
 * coords / 16): bottom slab + top slab (metal, 6 faces), four 2x12x2 corner posts (metal, 4 side
 * faces), and a glass interior cube (4 side faces). Metal renders in the solid pass (0), glass in
 * the alpha pass (1), dispatched via ForgeHooksClient.getWorldRenderPass() (BlockTank returns
 * getRenderBlockPass()==1 and canRenderInPass()==true so we are called in both passes).
 *
 * Faces are drawn with RenderBlocks.renderFace* with enableAO disabled, so each face uses the
 * brightness/color set on the Tessellator (AO color/brightness fields are only populated by
 * renderStandardBlock, which we do not use here). Per-face directional shading matches vanilla
 * (top 1.0, bottom 0.5, north/south 0.8, west/east 0.6) -- the same scheme
 * RenderBlocks.renderBlockSandFalling uses -- which keeps the frame correctly lit (no black faces).
 */
public class BlockTankRenderer implements ISimpleBlockRenderingHandler {

    protected static final double U = 1.0 / 16.0;

    // Packed full-bright brightness (skyLight=15, blockLight=15): (15 << 20) | (15 << 4). Used
    // for the item-icon overlay path (WAILA HUD) where there is no world to sample brightness
    // from; raw addVertexWithUV quads would otherwise render solid black (the classic 1.7.10
    // TESR brightness trap). Matches RenderTileTank.FULL_BRIGHT.
    protected static final int FULL_BRIGHT = 0x00F000F0;

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockTank)) return;
        // Default item icon: normal tank texture. The WAILA HUD path calls renderInventoryFrame
        // directly with the vending texture when the live tank has a Creative Vending upgrade.
        renderInventoryFrame((BlockTank) block, ((BlockTank) block).getIconTank(), renderer, true);
    }

    /**
     * Renders the inventory/item-icon tank frame + glass with the given metal-frame icon. The
     * default icon is {@link BlockTank#getIconTank()}; the WAILA HUD path passes
     * {@link BlockTank#getIconTankVending()} when the live tank has a Creative Vending upgrade,
     * mirroring {@link #renderWorldBlock}'s vending check on the in-world metal frame.
     */
    public void renderInventoryFrame(BlockTank tank, IIcon frameIcon, RenderBlocks renderer, boolean fullBright) {
        IIcon iconGlass = tank.getIconGlass();

        // The framework dispatches us via ForgeHooksClient.renderInventoryItem, which binds the
        // block atlas (for sprite number 0) and applies the isometric inventory transform, but does
        // NOT enable GL_BLEND for the INVENTORY type (unlike the ENTITY path, and unlike vanilla
        // RenderItem.renderItemIntoGUI's block path which the custom renderer replaces). So the
        // glass's transparent pixels would otherwise render opaque in the hotbar/inventory. We
        // replicate renderItemIntoGUI's blend + alphaFunc setup here (verified vs the decompiled
        // RenderItem.java: for getRenderBlockPass()!=0 it does glEnable(GL_ALPHA_TEST),
        // glAlphaFunc(GL_GREATER, 0.1F), glEnable(GL_BLEND), OpenGlHelper.glBlendFunc(770, 771, 1, 0)).
        //
        // Lighting (the "darker while holding any item" bug): the first-person held-item path
        // (ItemRenderer.renderItemInFirstPerson) does BOTH enableStandardItemLighting
        // (ItemRenderer:272, world-aligned GL lights) AND sets the lightmap to the player's AMBIENT
        // world light (ItemRenderer:287-290). Both leak past the hand render into the HUD hotbar
        // pass, darkening the icon whenever ANY item is held (an empty hand leaves the GUI full-bright
        // state, hence the symptom only appears while holding something). For fullBright=true
        // (inventory/hotbar/WAILA icon) we disable fixed-function GL_LIGHTING (removes the leaked
        // OpenGL-light darkening) and bake FULL_BRIGHT per-vertex via setBrightness (removes the
        // leaked-lightmap darkening). renderFace* (enableAO=false) does NOT call setBrightness itself
        // (verified vs decompiled RenderBlocks.renderFaceYNeg: the non-AO branch only does
        // addVertexWithUV), so the vertices inherit the Tessellator's current brightness -- baking
        // FULL_BRIGHT guarantees they sample the lightmap at (15,15) regardless of the current coord.
        // The EQUIPPED*/ENTITY types pass fullBright=false so the held/dropped item inherits the
        // ambient lightmap (dark at night), preserving the Finding-3 design and avoiding the
        // "glows near-white in darkness" regression from the prior unconditional setBrightness hardcode.
        // Per-face setColorOpaque_F directional shading still applies for the 3D look. Matches the
        // OpenBlocks TESR pattern (disable lighting, bake brightness).
        Tessellator tess = Tessellator.instance;
        boolean prevAO = renderer.enableAO;
        renderer.enableAO = false;

        // Scope {blend, blendFunc, alphaTest, alphaFunc, depthMask, colorWrite, lighting, lights}
        // so the caller's state is restored exactly -- no GL-state leak into later fullscreen passes
        // (a rare white-bar flash was observed when this state churned unsymmetrically). The
        // held-in-hand codepath (ItemRenderer.renderItem) sets glDepthMask(false) for any block
        // whose getRenderBlockPass()!=0; with depth-write off our opaque metal frame loses occlusion
        // and the corner posts (drawn after the top slab) bleed through the top face. We force
        // depth-write on here. The inventory-slot path leaves these already-on, so this is a no-op
        // there. Matches the SD TileEntityDrawersRenderer glPushAttrib pattern.
        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0); // GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA (mirrors renderItemIntoGUI)
        GL11.glDepthMask(true);
        if (fullBright) {
            GL11.glDisable(GL11.GL_LIGHTING);
        }

        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        tess.startDrawingQuads();
        if (fullBright) {
            // Bake full-bright (skyLight=15, blockLight=15) per-vertex so the quads sample the
            // lightmap at (15,15) regardless of the (possibly leaked-ambient) current coord.
            tess.setBrightness(FULL_BRIGHT);
        }
        renderMetalFrame(tank, frameIcon, renderer, 0.0, 0.0, 0.0);
        renderGlass(tank, iconGlass, renderer, 0.0, 0.0, 0.0);
        tess.draw();

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);

        // Reset the current color so a tinted color state cannot leak into a later pass.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
        renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        renderer.enableAO = prevAO;
    }

    /**
     * Convenience overload: default (non-forced-bright) inventory render, used by the ISBRH
     * fallback. Equivalent to {@code renderInventoryFrame(tank, frameIcon, renderer, false)}.
     */
    public void renderInventoryFrame(BlockTank tank, IIcon frameIcon, RenderBlocks renderer) {
        renderInventoryFrame(tank, frameIcon, renderer, false);
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        if (!(block instanceof BlockTank)) return false;
        BlockTank tank = (BlockTank) block;

        // Breaking-progress overlay: the chunk renderer sets an override (crack) texture; render a
        // full cube with it so the destruction animation shows.
        if (renderer.hasOverrideBlockTexture()) {
            renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
            renderer.renderStandardBlock(block, x, y, z);
            return true;
        }

        // Check the tile entity to see if vending upgrade is installed — if so,
        // use the vending texture for the metal frame.
        TileEntity te = world.getTileEntity(x, y, z);
        IIcon iconTank = (te instanceof TileTank && ((TileTank) te).isVending()) ? tank.getIconTankVending()
            : tank.getIconTank();
        IIcon iconGlass = tank.getIconGlass();
        Tessellator tess = Tessellator.instance;

        boolean prevAO = renderer.enableAO;
        renderer.enableAO = false;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));

        int pass = ForgeHooksClient.getWorldRenderPass();
        if (pass == 0) {
            renderMetalFrame(tank, iconTank, renderer, x, y, z);
        } else if (pass == 1) {
            renderGlass(tank, iconGlass, renderer, x, y, z);
            // Overlays drawn in the alpha pass so transparent icon pixels don't occlude the
            // frame/fluid behind them. Ports the 1.12.2 seal_part/lock_part/void_part submodels
            // + adds SD 1.7.10 claim icons + storage-trim overlays.
            if (te instanceof TileTank) {
                renderOverlays(tank, (TileTank) te, renderer, x, y, z);
            }
        }

        renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        renderer.enableAO = prevAO;
        return true;
    }

    protected void renderMetalFrame(BlockTank tank, IIcon icon, RenderBlocks renderer, double x, double y, double z) {
        // bottom slab (all 6 faces)
        drawBox(renderer, tank, icon, x, y, z, 0, 0, 0, 1, 2 * U, 1, true, true, true, true, true, true);
        // top slab (all 6 faces)
        drawBox(renderer, tank, icon, x, y, z, 0, 14 * U, 0, 1, 1, 1, true, true, true, true, true, true);
        // four corner posts (4 side faces each, no top/bottom -- matches the 1.12.2 model and
        // avoids z-fighting against the slab faces that share y=2*U / y=14*U)
        drawBox(renderer, tank, icon, x, y, z, 0, 2 * U, 0, 2 * U, 14 * U, 2 * U, false, false, true, true, true, true);
        drawBox(
            renderer,
            tank,
            icon,
            x,
            y,
            z,
            14 * U,
            2 * U,
            0,
            1,
            14 * U,
            2 * U,
            false,
            false,
            true,
            true,
            true,
            true);
        drawBox(
            renderer,
            tank,
            icon,
            x,
            y,
            z,
            0,
            2 * U,
            14 * U,
            2 * U,
            14 * U,
            1,
            false,
            false,
            true,
            true,
            true,
            true);
        drawBox(
            renderer,
            tank,
            icon,
            x,
            y,
            z,
            14 * U,
            2 * U,
            14 * U,
            1,
            14 * U,
            1,
            false,
            false,
            true,
            true,
            true,
            true);
    }

    protected void renderGlass(BlockTank tank, IIcon icon, RenderBlocks renderer, double x, double y, double z) {
        // inner glass cube (4 side faces only, no top/bottom -- matches the 1.12.2 model)
        drawBox(
            renderer,
            tank,
            icon,
            x,
            y,
            z,
            1 * U,
            1 * U,
            1 * U,
            15 * U,
            15 * U,
            15 * U,
            false,
            false,
            true,
            true,
            true,
            true);
    }

    // --- Phase 11/10 overlay rendering ---

    protected void renderOverlays(BlockTank tank, TileTank tile, RenderBlocks renderer, double x, double y, double z) {
        Tessellator tess = Tessellator.instance;
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);

        // Full-face overlays: seal/tape + storage trim (drawn slightly outside the block).
        if (tile.isSealed()) {
            drawFullFaceOverlay(renderer, tank, tank.getIconTape(), x, y, z);
        }
        int trimLevel = tile.getStorageTrimLevel();
        if (trimLevel >= 2) {
            IIcon trimIcon = tank.getIconTrim(trimLevel);
            if (trimIcon != null) {
                drawFullFaceOverlay(renderer, tank, trimIcon, x, y, z);
            }
        }

        // Small indicator icons: lock/claim (top-center) + void (top-corner).
        boolean locked = tile.getAttributes()
            .isItemLocked(LockAttribute.LOCK_POPULATED)
            || tile.getAttributes()
                .isItemLocked(LockAttribute.LOCK_EMPTY);
        boolean hasOwner = tile.getOwner() != null;
        if (locked || hasOwner) {
            IIcon icon;
            if (locked && hasOwner) icon = tank.getIconClaimLock();
            else if (locked) icon = tank.getIconLock();
            else icon = tank.getIconClaim();
            // Lock/claim: centered on each side (1.12.2 lock_part.json: x/z 7.25-8.75, y 14.25-15.75)
            double c = 7.25 * U, d = 8.75 * U;
            drawIconQuads(tess, icon, x, y, z, c, d, c, d, c, d, c, d, 14.25 * U, 15.75 * U);
        }
        if (tile.getAttributes()
            .isVoid()) {
            // Void: corner of each side (1.12.2 void_part.json: N x=0.25-1.75, S x=14.25-15.75, etc.)
            drawIconQuads(
                tess,
                tank.getIconVoid(),
                x,
                y,
                z,
                0.25 * U,
                1.75 * U, // N: left corner
                14.25 * U,
                15.75 * U, // S: right corner
                14.25 * U,
                15.75 * U, // W: back corner
                0.25 * U,
                1.75 * U, // E: front corner
                14.25 * U,
                15.75 * U);
        }
    }

    /**
     * Renders the seal/trim/lock/void overlays for an item icon (WAILA HUD), mirroring
     * {@link #renderWorldBlock}'s alpha-pass overlay pass but in self-contained Tessellator
     * batches suitable for the inventory/item render path (which has no chunk-renderer-managed
     * open batch). {@code tile} is a transient {@link TileTank} reconstructed from the item's
     * portable NBT (worldObj == null); only portable state is read, so this is null-world-safe.
     *
     * <p>
     * Drawn in block-local {@code 0..1} space at the origin; the caller wraps this in a
     * {@code -0.5} translate (same as {@link #renderInventoryBlock}). Brightness is forced
     * full-bright (GUI HUD icon); blend is enabled so the transparent pixels of the
     * lock/claim/void indicator icons blend correctly (matches the in-world alpha pass).
     */
    public void renderInventoryOverlays(BlockTank tank, TileTank tile, RenderBlocks renderer) {
        Tessellator tess = Tessellator.instance;
        boolean prevAO = renderer.enableAO;
        renderer.enableAO = false;

        boolean prevBlend = GL11.glGetBoolean(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        // Full-face overlays: seal/tape + storage trim (drawn slightly outside the block).
        tess.startDrawingQuads();
        tess.setBrightness(FULL_BRIGHT);
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        if (tile.isSealed()) {
            drawFullFaceOverlay(renderer, tank, tank.getIconTape(), 0, 0, 0);
        }
        int trimLevel = tile.getStorageTrimLevel();
        if (trimLevel >= 2) {
            IIcon trimIcon = tank.getIconTrim(trimLevel);
            if (trimIcon != null) {
                drawFullFaceOverlay(renderer, tank, trimIcon, 0, 0, 0);
            }
        }
        tess.draw();

        // Small indicator icons: lock/claim (top-center) + void (top-corner).
        boolean locked = tile.getAttributes()
            .isItemLocked(LockAttribute.LOCK_POPULATED)
            || tile.getAttributes()
                .isItemLocked(LockAttribute.LOCK_EMPTY);
        boolean hasOwner = tile.getOwner() != null;
        if (locked || hasOwner) {
            IIcon icon;
            if (locked && hasOwner) icon = tank.getIconClaimLock();
            else if (locked) icon = tank.getIconLock();
            else icon = tank.getIconClaim();
            // Lock/claim: centered on each side (1.12.2 lock_part.json: x/z 7.25-8.75, y 14.25-15.75)
            double c = 7.25 * U, d = 8.75 * U;
            tess.startDrawingQuads();
            tess.setBrightness(FULL_BRIGHT);
            tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            drawIconQuads(tess, icon, 0, 0, 0, c, d, c, d, c, d, c, d, 14.25 * U, 15.75 * U);
            tess.draw();
        }
        if (tile.getAttributes()
            .isVoid()) {
            // Void: corner of each side (1.12.2 void_part.json: N x=0.25-1.75, S x=14.25-15.75, etc.)
            tess.startDrawingQuads();
            tess.setBrightness(FULL_BRIGHT);
            tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            drawIconQuads(
                tess,
                tank.getIconVoid(),
                0,
                0,
                0,
                0.25 * U,
                1.75 * U, // N: left corner
                14.25 * U,
                15.75 * U, // S: right corner
                14.25 * U,
                15.75 * U, // W: back corner
                0.25 * U,
                1.75 * U, // E: front corner
                14.25 * U,
                15.75 * U);
            tess.draw();
        }

        if (!prevBlend) GL11.glDisable(GL11.GL_BLEND);
        renderer.enableAO = prevAO;
    }

    /**
     * Draws a full-face overlay (tape or storage trim) on all 4 side faces, slightly outside the
     * block to avoid z-fighting. Uses RenderBlocks.renderFace* which maps the full-block face
     * (0..1) to the full icon UV.
     */
    private void drawFullFaceOverlay(RenderBlocks renderer, Block block, IIcon icon, double x, double y, double z) {
        Tessellator tess = Tessellator.instance;
        tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
        double off = 0.003;
        renderer.setRenderBounds(0, 0, -off, 1, 1, 1);
        renderer.renderFaceZNeg(block, x, y, z, icon);
        renderer.setRenderBounds(0, 0, 0, 1, 1, 1 + off);
        renderer.renderFaceZPos(block, x, y, z, icon);
        renderer.setRenderBounds(-off, 0, 0, 1, 1, 1);
        renderer.renderFaceXNeg(block, x, y, z, icon);
        renderer.setRenderBounds(0, 0, 0, 1 + off, 1, 1);
        renderer.renderFaceXPos(block, x, y, z, icon);
    }

    /**
     * Draws small indicator-icon quads on all 4 side faces with the full icon UV mapped to each
     * small quad (RenderBlocks.renderFace* can't do this -- it maps UV from block position).
     * Positions ported from the 1.12.2 lock_part.json / void_part.json (in 1/16th units).
     *
     * <p>
     * IMPORTANT: this is called from {@link #renderWorldBlock}, which runs inside the chunk
     * renderer's already-open Tessellator batch. We must only add vertices -- calling
     * startDrawingQuads/draw here would crash with "Already tesselating!". The chunk renderer
     * opened the batch and will draw it.
     */
    private void drawIconQuads(Tessellator tess, IIcon icon, double x, double y, double z, double nxMin, double nxMax,
        double sxMin, double sxMax, double wzMin, double wzMax, double ezMin, double ezMax, double yMin, double yMax) {
        double off = 0.003;
        double uMin = icon.getMinU(), uMax = icon.getMaxU();
        double vMin = icon.getMinV(), vMax = icon.getMaxV();

        // North face (-Z) -- 4 vertices = 1 quad, added to the chunk renderer's open batch.
        tess.addVertexWithUV(x + nxMin, y + yMin, z - off, uMin, vMax);
        tess.addVertexWithUV(x + nxMax, y + yMin, z - off, uMax, vMax);
        tess.addVertexWithUV(x + nxMax, y + yMax, z - off, uMax, vMin);
        tess.addVertexWithUV(x + nxMin, y + yMax, z - off, uMin, vMin);

        // South face (+Z)
        tess.addVertexWithUV(x + sxMin, y + yMin, z + 1 + off, uMin, vMax);
        tess.addVertexWithUV(x + sxMin, y + yMax, z + 1 + off, uMin, vMin);
        tess.addVertexWithUV(x + sxMax, y + yMax, z + 1 + off, uMax, vMin);
        tess.addVertexWithUV(x + sxMax, y + yMin, z + 1 + off, uMax, vMax);

        // West face (-X)
        tess.addVertexWithUV(x - off, y + yMin, z + wzMin, uMin, vMax);
        tess.addVertexWithUV(x - off, y + yMin, z + wzMax, uMax, vMax);
        tess.addVertexWithUV(x - off, y + yMax, z + wzMax, uMax, vMin);
        tess.addVertexWithUV(x - off, y + yMax, z + wzMin, uMin, vMin);

        // East face (+X)
        tess.addVertexWithUV(x + 1 + off, y + yMin, z + ezMin, uMin, vMax);
        tess.addVertexWithUV(x + 1 + off, y + yMax, z + ezMin, uMin, vMin);
        tess.addVertexWithUV(x + 1 + off, y + yMax, z + ezMax, uMax, vMin);
        tess.addVertexWithUV(x + 1 + off, y + yMin, z + ezMax, uMax, vMax);
    }

    /**
     * Draws the selected faces of an axis-aligned box using RenderBlocks.renderFace* with enableAO
     * disabled. Brightness is whatever the caller set on the Tessellator (full-bright for inventory,
     * getMixedBrightnessForBlock for world). Per-face directional shading is applied so faces are
     * lit rather than flat/black.
     */
    protected void drawBox(RenderBlocks renderer, Block block, IIcon icon, double x, double y, double z, double minX,
        double minY, double minZ, double maxX, double maxY, double maxZ, boolean bottom, boolean top, boolean north,
        boolean south, boolean west, boolean east) {
        renderer.setRenderBounds(minX, minY, minZ, maxX, maxY, maxZ);
        Tessellator tess = Tessellator.instance;
        if (bottom) {
            tess.setColorOpaque_F(0.5F, 0.5F, 0.5F);
            renderer.renderFaceYNeg(block, x, y, z, icon);
        }
        if (top) {
            tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
            renderer.renderFaceYPos(block, x, y, z, icon);
        }
        if (north) {
            tess.setColorOpaque_F(0.8F, 0.8F, 0.8F);
            renderer.renderFaceZNeg(block, x, y, z, icon);
        }
        if (south) {
            tess.setColorOpaque_F(0.8F, 0.8F, 0.8F);
            renderer.renderFaceZPos(block, x, y, z, icon);
        }
        if (west) {
            tess.setColorOpaque_F(0.6F, 0.6F, 0.6F);
            renderer.renderFaceXNeg(block, x, y, z, icon);
        }
        if (east) {
            tess.setColorOpaque_F(0.6F, 0.6F, 0.6F);
            renderer.renderFaceXPos(block, x, y, z, icon);
        }
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return ModBlocks.tankRenderId;
    }
}
