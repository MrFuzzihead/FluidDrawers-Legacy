package com.mrfuzzihead.fluiddrawers.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;

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

    private static final double U = 1.0 / 16.0;

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockTank)) return;
        BlockTank tank = (BlockTank) block;
        IIcon iconTank = tank.getIconTank();
        IIcon iconGlass = tank.getIconGlass();

        // The framework (RenderItem.renderBlockAsItem -> custom dispatch) calls us WITHOUT a
        // startDrawingQuads/draw wrapper and has already bound the block atlas + set up the
        // isometric view (and blend, since getRenderBlockPass()==1). We manage tessellation
        // ourselves and center the 0..1 model at the origin with a -0.5 translate.
        //
        // Brightness is NOT forced here. renderBlockAsItem does not set a brightness for the custom
        // path, so the quads inherit the lightmap current coord set by each caller:
        // - Inventory slot: GuiContainer sets setLightmapTextureCoords(240, 240) = full bright
        // (GuiContainer:105-107), so the icon stays fully lit.
        // - Held in hand: ItemRenderer.renderItemInFirstPerson sets the lightmap to the player's
        // ambient world light (ItemRenderer:287-290), so the held tank is dark at night.
        // - Dropped entity: ambient world light, so it is dark at night.
        // Forcing setBrightness(15728880) here previously made the item glow full-bright (near-white
        // in complete darkness) in the hand and on the ground. Per-face directional shading
        // (setColorOpaque_F in drawBox) is still applied for the 3D look. Matches the OpenBlocks
        // pattern (BlockProjectorRenderer / ItemRendererTank do not force item brightness).
        Tessellator tess = Tessellator.instance;
        boolean prevAO = renderer.enableAO;
        renderer.enableAO = false;

        // The held-in-hand codepath (ItemRenderer.renderItemInFirstPerson) calls renderBlockAsItem
        // with depth-write DISABLED (glDepthMask(false)) for any block whose getRenderBlockPass()
        // != 0 -- see ItemRenderer.java:95. With depth-write off our opaque metal frame loses
        // occlusion, so the corner posts (drawn after the top slab) bleed through the top face.
        // Re-enable depth-write for our render and restore the caller's state afterwards. The
        // inventory-slot path leaves depth-write on, so this save/restore is a no-op there.
        boolean prevDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        GL11.glDepthMask(true);

        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        tess.startDrawingQuads();
        // No setBrightness: use the ambient/GUI lightmap (see comment above). renderFace* with
        // enableAO=false emits vertices carrying the per-face setColorOpaque_F directional shade
        // and the caller's lightmap current coord.
        renderMetalFrame(tank, iconTank, renderer, 0.0, 0.0, 0.0);
        renderGlass(tank, iconGlass, renderer, 0.0, 0.0, 0.0);
        tess.draw();

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);

        GL11.glDepthMask(prevDepthMask);
        renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        renderer.enableAO = prevAO;
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

        IIcon iconTank = tank.getIconTank();
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
        }

        renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        renderer.enableAO = prevAO;
        return true;
    }

    private void renderMetalFrame(BlockTank tank, IIcon icon, RenderBlocks renderer, double x, double y, double z) {
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

    private void renderGlass(BlockTank tank, IIcon icon, RenderBlocks renderer, double x, double y, double z) {
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

    /**
     * Draws the selected faces of an axis-aligned box using RenderBlocks.renderFace* with enableAO
     * disabled. Brightness is whatever the caller set on the Tessellator (full-bright for inventory,
     * getMixedBrightnessForBlock for world). Per-face directional shading is applied so faces are
     * lit rather than flat/black.
     */
    private void drawBox(RenderBlocks renderer, Block block, IIcon icon, double x, double y, double z, double minX,
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
