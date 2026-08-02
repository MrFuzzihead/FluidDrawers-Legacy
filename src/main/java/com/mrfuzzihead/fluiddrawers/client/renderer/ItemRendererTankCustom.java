package com.mrfuzzihead.fluiddrawers.client.renderer;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.IItemRenderer;

import org.lwjgl.opengl.GL11;

import com.mrfuzzihead.fluiddrawers.block.BlockTankCustom;
import com.mrfuzzihead.fluiddrawers.client.tesr.RenderTileTank;
import com.mrfuzzihead.fluiddrawers.init.ModBlocks;
import com.mrfuzzihead.fluiddrawers.tile.TileTankCustom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Custom item renderer for the framed Fluid Tank. Reads the {@code MatS}/{@code MatT} materials
 * from the ItemStack and renders the frame (side) + trim border (trim) + glass via
 * {@link BlockTankCustomRenderer#renderInventoryShape}. Sealed framed tank items additionally draw
 * the stored fluid and the seal/lock/void overlays (reusing the basic tank's TESR + overlay paths),
 * so the item icon matches the in-world framed tank.
 */
@SideOnly(Side.CLIENT)
public class ItemRendererTankCustom implements IItemRenderer {

    private static final BlockTankCustomRenderer ISBRH = new BlockTankCustomRenderer();

    @Override
    public boolean handleRenderType(ItemStack item, ItemRenderType type) {
        return true;
    }

    @Override
    public boolean shouldUseRenderHelper(ItemRenderType type, ItemStack item, ItemRendererHelper helper) {
        return true;
    }

    @Override
    public void renderItem(ItemRenderType type, ItemStack item, Object... data) {
        BlockTankCustom block = ModBlocks.TANK_CUSTOM;
        RenderBlocks renderer = (data != null && data.length > 0 && data[0] instanceof RenderBlocks)
            ? (RenderBlocks) data[0]
            : RenderBlocks.getInstance();

        // Inventory / hotbar icons are full-bright; held/dropped items inherit the ambient lightmap.
        boolean inventoryFullBright = (type == ItemRenderType.INVENTORY);

        NBTTagCompound tag = item.getTagCompound();
        boolean hasTile = tag != null && tag.hasKey("Tile", 10);

        if (hasTile) {
            TileTankCustom tile = new TileTankCustom();
            tile.readFromPortableNBT(tag.getCompoundTag("Tile"));

            IIcon sideIcon = resolve(tile.getMaterialSide(), block.getDefaultTexture());
            IIcon trimIcon = resolve(tile.getMaterialTrim(), block.getDefaultTexture());
            ISBRH.renderInventoryShape(block, sideIcon, trimIcon, renderer, inventoryFullBright);

            // Fluid + overlays are drawn in block-local 0..1 space; wrap in a -0.5 translate.
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            RenderTileTank.renderFluidForItem(tile);
            ISBRH.renderInventoryOverlays(block, tile, renderer);
            GL11.glTranslatef(0.5F, 0.5F, 0.5F);
        } else {
            IIcon sideIcon = resolve(readMaterial(tag, "MatS"), block.getDefaultTexture());
            IIcon trimIcon = resolve(readMaterial(tag, "MatT"), block.getDefaultTexture());
            ISBRH.renderInventoryShape(block, sideIcon, trimIcon, renderer, inventoryFullBright);
        }
    }

    private static ItemStack readMaterial(NBTTagCompound tag, String key) {
        if (tag == null || !tag.hasKey(key, 10)) return null;
        return ItemStack.loadItemStackFromNBT(tag.getCompoundTag(key));
    }

    private static IIcon resolve(ItemStack stack, IIcon fallback) {
        return BlockTankCustomRenderer.resolveMaterial(stack, fallback);
    }
}
