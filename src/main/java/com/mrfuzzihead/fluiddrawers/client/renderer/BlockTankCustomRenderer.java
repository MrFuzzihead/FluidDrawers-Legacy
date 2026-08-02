package com.mrfuzzihead.fluiddrawers.client.renderer;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.block.BlockTankCustom;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.item.block.ItemBlockTankCustom;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;
import com.mrfuzzihead.fluiddrawers.tile.TileTankCustom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Custom ISBRH for the framed Fluid Tank. Extends {@link BlockTankRenderer} and re-uses its
 * frame/glass geometry, but renders the main frame with the <b>side</b> material and an added
 * outer <b>trim</b> border with the <b>trim</b> material (ports the {@code #side}/{@code #trim}
 * elements of the 1.12.2 {@code tank_custom_base.json}). The interior pane keeps the glass texture
 * (the framed model's {@code #front}; glass tint deferred).
 *
 * <p>
 * {@link #renderInventoryShape} renders the whole framed tank (frame + trim + glass) in
 * inventory/item space so the item icon can be drawn by {@link ItemRendererTankCustom} with the
 * materials read from the ItemStack.
 */
@SideOnly(Side.CLIENT)
public class BlockTankCustomRenderer extends BlockTankRenderer {

    // 0.05px inset applied to the frame so its outer faces sit inside the trim ring (which occupies
    // the outer 0..1px). Without this the frame's outer faces (at 0) are coplanar with the trim
    // pillars'/edge-bars' outer faces (also at 0) and Z-fight. Mirrors the 0.05 offsets in the
    // 1.12.2 tank_custom_base.json frame elements. (1px = U block units.)
    private static final double S = 0.05 * U;

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockTankCustom)) return;
        BlockTankCustom tank = (BlockTankCustom) block;
        // Default (raw / un-framed) framed tank for creative tab / block picker.
        renderInventoryShape(
            tank,
            tank.getDefaultTexture(),
            tank.getDefaultTexture(),
            tank.getIconGlass(),
            renderer,
            true);
    }

    /**
     * Renders the framed tank (frame + trim + glass) in inventory/item icon space, mirroring
     * {@link BlockTankRenderer#renderInventoryFrame}'s GL setup but drawing the frame with
     * {@code sideIcon}, the trim border with {@code trimIcon}, and the window with {@code glassIcon}
     * (the front material's own icon -- clear glass is plain, stained glass is the baked-colour
     * texture).
     */
    public void renderInventoryShape(BlockTankCustom tank, IIcon sideIcon, IIcon trimIcon, IIcon glassIcon,
        RenderBlocks renderer, boolean fullBright) {
        Tessellator tess = Tessellator.instance;
        boolean prevAO = renderer.enableAO;
        renderer.enableAO = false;

        GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_LIGHTING_BIT);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glDepthMask(true);
        if (fullBright) {
            GL11.glDisable(GL11.GL_LIGHTING);
        }

        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);

        tess.startDrawingQuads();
        if (fullBright) {
            tess.setBrightness(FULL_BRIGHT);
        }
        renderFramedFrame(tank, sideIcon, renderer, 0.0, 0.0, 0.0);
        renderTrim(tank, trimIcon, renderer, 0.0, 0.0, 0.0);
        renderGlass(tank, glassIcon, renderer, 0.0, 0.0, 0.0);
        tess.draw();

        GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
        renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        renderer.enableAO = prevAO;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        if (!(block instanceof BlockTankCustom)) return false;
        BlockTankCustom tank = (BlockTankCustom) block;

        // Breaking-progress overlay: render the full cube with the crack texture.
        if (renderer.hasOverrideBlockTexture()) {
            renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
            renderer.renderStandardBlock(block, x, y, z);
            return true;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        TileTankCustom tile = te instanceof TileTankCustom ? (TileTankCustom) te : null;

        IIcon sideIcon = (tile != null && tile.getMaterialSide() != null)
            ? resolveMaterial(tile.getMaterialSide(), tank.getDefaultTexture())
            : tank.getDefaultTexture();
        IIcon trimIcon = (tile != null && tile.getMaterialTrim() != null)
            ? resolveMaterial(tile.getMaterialTrim(), tank.getDefaultTexture())
            : tank.getDefaultTexture();

        Tessellator tess = Tessellator.instance;
        boolean prevAO = renderer.enableAO;
        renderer.enableAO = false;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));

        int pass = ForgeHooksClient.getWorldRenderPass();
        if (pass == 0) {
            renderFramedFrame(tank, sideIcon, renderer, x, y, z);
            renderTrim(tank, trimIcon, renderer, x, y, z);
        } else if (pass == 1) {
            // Alpha pass: window + indicators. The front material is the window -- its own icon
            // carries the colour (clear glass is plain, stained glass is the baked-colour texture).
            IIcon glassIcon = tile != null && tile.getMaterialFront() != null
                ? resolveGlassIcon(tile.getMaterialFront(), tank.getIconGlass())
                : tank.getIconGlass();
            renderGlass(tank, glassIcon, renderer, x, y, z);
            if (te instanceof TileTank) {
                renderOverlays(tank, (TileTank) te, renderer, x, y, z);
            }
        }

        renderer.setRenderBounds(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
        renderer.enableAO = prevAO;
        return true;
    }

    /**
     * Draws the framed tank's main frame (bottom/top slabs + four corner posts) with the given
     * side-material icon, inset by {@link #S} (0.05px) so the outer faces sit inside the 1px trim
     * ring instead of being coplanar with it (avoids Z-fighting). Ports the {@code #side} elements
     * of {@code tank_custom_base.json}. Block-local 0..1 space at (x, y, z); must be called inside
     * an open Tessellator batch.
     */
    protected void renderFramedFrame(BlockTank tank, IIcon icon, RenderBlocks renderer, double x, double y, double z) {
        // Bottom slab (inset 0.05px on all outer faces; y 0.05px..2px).
        drawBox(renderer, tank, icon, x, y, z, S, S, S, 1 - S, 2 * U, 1 - S, true, true, true, true, true, true);
        // Top slab (y 14px..15.95px).
        drawBox(renderer, tank, icon, x, y, z, S, 14 * U, S, 1 - S, 1 - S, 1 - S, true, true, true, true, true, true);
        // Four corner posts (2px wide, y 2px..14px, 4 side faces).
        drawBox(renderer, tank, icon, x, y, z, S, 2 * U, S, 2 * U, 14 * U, 2 * U, false, false, true, true, true, true);
        drawBox(
            renderer,
            tank,
            icon,
            x,
            y,
            z,
            14 * U,
            2 * U,
            S,
            1 - S,
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
            S,
            2 * U,
            14 * U,
            2 * U,
            14 * U,
            1 - S,
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
            1 - S,
            14 * U,
            1 - S,
            false,
            false,
            true,
            true,
            true,
            true);
    }

    /**
     * Draws the outer 1px trim border using the given icon: bottom/top edge bars (14 wide) and four
     * full-height corner pillars. Ports the {@code #trim} elements of {@code tank_custom_base.json}
     * -- each element draws only its outward-facing faces (the inward faces are intentionally not
     * rendered: they sit inside the solid frame/glass, so there is no inner face to see). The frame
     * is inset by {@link #S}, so these outer faces at 0/1 are never coplanar with the frame.
     * Block-local 0..1 space at (x, y, z); must be called inside an open Tessellator batch.
     */
    public void renderTrim(BlockTank tank, IIcon icon, RenderBlocks renderer, double x, double y, double z) {
        double e = U;

        // Bottom edge bars (y 0..1), 14 wide.
        drawBox(renderer, tank, icon, x, y, z, 0, 0, e, e, e, 15 * e, true, false, false, false, true, false);
        drawBox(renderer, tank, icon, x, y, z, e, 0, 0, 15 * e, e, e, true, false, true, false, false, false);
        drawBox(renderer, tank, icon, x, y, z, 15 * e, 0, e, 1, e, 15 * e, true, false, false, false, false, true);
        drawBox(renderer, tank, icon, x, y, z, e, 0, 15 * e, 15 * e, e, 1, true, false, false, true, false, false);

        // Top edge bars (y 15..16), 14 wide.
        drawBox(renderer, tank, icon, x, y, z, 0, 15 * e, e, e, 1, 15 * e, false, true, false, false, true, false);
        drawBox(renderer, tank, icon, x, y, z, e, 15 * e, 0, 15 * e, 1, e, false, true, true, false, false, false);
        drawBox(renderer, tank, icon, x, y, z, 15 * e, 15 * e, e, 1, 1, 15 * e, false, true, false, false, false, true);
        drawBox(renderer, tank, icon, x, y, z, e, 15 * e, 15 * e, 15 * e, 1, 1, false, true, false, true, false, false);

        // Four full-height corner pillars (1 x 16 x 1).
        drawBox(renderer, tank, icon, x, y, z, 0, 0, 0, e, 1, e, true, true, true, false, true, false);
        drawBox(renderer, tank, icon, x, y, z, 15 * e, 0, 0, 1, 1, e, true, true, true, false, false, true);
        drawBox(renderer, tank, icon, x, y, z, 0, 0, 15 * e, e, 1, 1, true, true, false, true, true, false);
        drawBox(renderer, tank, icon, x, y, z, 15 * e, 0, 15 * e, 1, 1, 1, true, true, false, true, false, true);
    }

    /**
     * Resolves the icon for the tank's front/window material. Only glass materials change the
     * window: the material's own icon is used, so clear glass/pane gives the plain glass texture and
     * stained glass/pane gives its baked-colour texture ({@code glass_<color>}). 1.7.10 stores the
     * stained-glass colour in the texture (see {@code BlockStainedGlass.getIcon}/{@code registerBlockIcons};
     * {@code Block.getRenderColor} stays white for these). Non-glass materials fall back to clear glass.
     */
    public static IIcon resolveGlassIcon(ItemStack frontMaterial, IIcon fallback) {
        if (!ItemBlockTankCustom.isGlassMaterial(frontMaterial)) return fallback;
        Block block = Block.getBlockFromItem(frontMaterial.getItem());
        if (block == null) return fallback;
        IIcon icon = block.getIcon(4, frontMaterial.getItemDamage());
        return icon != null ? icon : fallback;
    }

    public static IIcon resolveMaterial(ItemStack stack, IIcon fallback) {
        if (stack == null || stack.getItem() == null) return fallback;
        Block b = Block.getBlockFromItem(stack.getItem());
        if (b == null) return fallback;
        IIcon icon = b.getIcon(4, stack.getItemDamage());
        return icon != null ? icon : fallback;
    }

    @Override
    public int getRenderId() {
        return ModBlocks.tankCustomRenderId;
    }

}
