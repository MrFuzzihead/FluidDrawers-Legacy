package com.mrfuzzihead.fluiddrawers.item.block;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.mrfuzzihead.fluiddrawers.tile.TileTankCustom;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * ItemBlock for the framed Fluid Tank. Ports the 1.12.2 {@code ItemBlockTankCustom} (which
 * implemented the {@code FramedItem} interface against Chameleon's {@code MaterialData}) to the
 * 1.7.10 StorageDrawers convention of storing materials in the item's top-level NBT
 * ({@code MatS}/{@code MatT}/{@code MatF}), the same format {@code ItemCustomDrawers.makeItemStack}
 * produces.
 *
 * <p>
 * Responsibilities:
 * <ul>
 * <li><b>Place:</b> apply the {@code MatS}/{@code MatT}/{@code MatF} materials from the stack to
 * the freshly-placed {@link TileTankCustom} (after the base {@code ItemBlockTank} has restored the
 * sealed portable NBT).</li>
 * <li><b>Framing table:</b> static {@link #makeFramedTankStack} builds a framed-tank item from
 * input materials, invoked by the {@code MixinContainerFramingTable} late mixin.</li>
 * <li><b>Tooltip:</b> list the applied materials alongside the base tank tooltip.</li>
 * </ul>
 */
public class ItemBlockTankCustom extends ItemBlockTank {

    public ItemBlockTankCustom(Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (!super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, metadata)) {
            return false;
        }

        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileTankCustom) {
            applyMaterials((TileTankCustom) te, stack.getTagCompound());
        }

        return true;
    }

    /**
     * Applies the {@code MatS}/{@code MatT}/{@code MatF} materials from an item's NBT to a tile.
     * Null-safe: missing tags are left untouched.
     */
    public static void applyMaterials(TileTankCustom tile, NBTTagCompound tag) {
        if (tile == null || tag == null) return;
        if (tag.hasKey("MatS", 10)) tile.setMaterialSide(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatS")));
        if (tag.hasKey("MatT", 10)) tile.setMaterialTrim(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatT")));
        if (tag.hasKey("MatF", 10)) tile.setMaterialFront(ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatF")));
    }

    /**
     * Builds a framed-tank item carrying the given materials, mirroring
     * {@code ItemCustomDrawers.makeItemStack} (materials stored as {@code MatS}/{@code MatT}/
     * {@code MatF}, each a single-item compound). Used by the framing-table mixin.
     */
    public static ItemStack makeFramedTankStack(Block block, int count, ItemStack matSide, ItemStack matTrim,
        ItemStack matFront) {
        NBTTagCompound tag = new NBTTagCompound();
        if (matSide != null) tag.setTag("MatS", materialTag(matSide));
        if (matTrim != null) tag.setTag("MatT", materialTag(matTrim));
        if (matFront != null) tag.setTag("MatF", materialTag(matFront));

        ItemStack stack = new ItemStack(Item.getItemFromBlock(block), count, 0);
        if (!tag.hasNoTags()) stack.setTagCompound(tag);
        return stack;
    }

    private static NBTTagCompound materialTag(ItemStack mat) {
        mat = mat.copy();
        mat.stackSize = 1;
        return mat.writeToNBT(new NBTTagCompound());
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        super.addInformation(stack, player, list, advanced);

        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) return;

        boolean any = false;
        ItemStack matSide = null, matTrim = null, matFront = null;
        if (tag.hasKey("MatS", 10)) {
            matSide = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatS"));
            any = true;
        }
        if (tag.hasKey("MatT", 10)) {
            matTrim = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatT"));
            any = true;
        }
        if (tag.hasKey("MatF", 10)) {
            matFront = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatF"));
            any = true;
        }
        if (!any) return;

        list.add(EnumChatFormatting.GRAY + StatCollector.translateToLocal("fluiddrawers.tooltip.materials"));
        if (matSide != null) {
            list.add(
                "  " + EnumChatFormatting.YELLOW
                    + StatCollector.translateToLocal("fluiddrawers.tooltip.materialSide")
                    + " "
                    + matSide.getDisplayName());
        }
        if (matTrim != null) {
            list.add(
                "  " + EnumChatFormatting.YELLOW
                    + StatCollector.translateToLocal("fluiddrawers.tooltip.materialTrim")
                    + " "
                    + matTrim.getDisplayName());
        }
        if (matFront != null) {
            list.add(
                "  " + EnumChatFormatting.YELLOW
                    + StatCollector.translateToLocal("fluiddrawers.tooltip.materialFront")
                    + " "
                    + matFront.getDisplayName());
        }
    }
}
