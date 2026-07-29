package com.mrfuzzihead.fluiddrawers.client.renderer;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.fluiddrawers.block.BlockTank;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Custom item renderer for the Fluid Tank. Shows the tape overlay on sealed tank items in the
 * inventory, held in hand, and dropped on the ground -- the 1.7.10 equivalent of the 1.12.2
 * {@code tank_sealed.json} item model (which combined the tank + seal_part models via libnine's
 * parameterized model system).
 *
 * <p>
 * Delegates the base frame + glass rendering to {@link BlockTankRenderer#renderInventoryBlock},
 * then draws the tape overlay (storagedrawers:tape) on all 4 side faces if the ItemStack carries a
 * {@code "Tile"} portable NBT subtag (i.e. the tank was sealed when broken).
 */
@SideOnly(Side.CLIENT)
public class ItemRendererTank implements IItemRenderer {

    // BlockTankRenderer is stateless, so a single shared instance is safe.
    private static final BlockTankRenderer ISBRH = new BlockTankRenderer();

    private static final double U = 1.0 / 16.0;

    @Override
    public boolean handleRenderType(ItemStack item, IItemRenderer.ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(IItemRenderer.ItemRenderType type, ItemStack item,
        IItemRenderer.ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(IItemRenderer.ItemRenderType type, ItemStack stack, Object... data) {
        BlockTank block = ModBlocks.TANK;
        RenderBlocks renderer = (data != null && data.length > 0 && data[0] instanceof RenderBlocks)
            ? (RenderBlocks) data[0]
            : RenderBlocks.getInstance();

        // Draw the base frame + glass (same as the ISBRH's renderInventoryBlock).
        ISBRH.renderInventoryBlock(block, 0, ModBlocks.tankRenderId, renderer);

        // If the item is sealed (has "Tile" portable NBT), draw the tape overlay on top.
        // renderInventoryBlock opened + closed its own Tessellator batch (startDrawingQuads/draw),
        // so we need a fresh batch for the overlay quads.
        NBTTagCompound tag = stack.getTagCompound();
        if (tag != null && tag.hasKey("Tile", 10)) {
            IIcon tapeIcon = block.getIconTape();
            if (tapeIcon != null) {
                GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
                Tessellator tess = Tessellator.instance;
                tess.startDrawingQuads();
                tess.setColorOpaque_F(1.0F, 1.0F, 1.0F);
                double off = 0.003;
                renderer.setRenderBounds(0, 0, -off, 1, 1, 1);
                renderer.renderFaceZNeg(block, 0, 0, 0, tapeIcon);
                renderer.setRenderBounds(0, 0, 0, 1, 1, 1 + off);
                renderer.renderFaceZPos(block, 0, 0, 0, tapeIcon);
                renderer.setRenderBounds(-off, 0, 0, 1, 1, 1);
                renderer.renderFaceXNeg(block, 0, 0, 0, tapeIcon);
                renderer.setRenderBounds(0, 0, 0, 1 + off, 1, 1);
                renderer.renderFaceXPos(block, 0, 0, 0, tapeIcon);
                tess.draw();
                GL11.glTranslatef(0.5F, 0.5F, 0.5F);
            }
        }
    }
}
