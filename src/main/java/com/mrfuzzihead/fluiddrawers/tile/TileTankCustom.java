package com.mrfuzzihead.fluiddrawers.tile;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Tile entity for the framed Fluid Tank. Extends {@link TileTank} and adds the three frame
 * materials (side / trim / front) stored as ItemStacks, porting the 1.12.2
 * {@code TileTankCustom} (which delegated to StorageDrawers' {@code MaterialData}) to the
 * 1.7.10 StorageDrawers convention of inlining the materials directly ({@code MatS}/{@code MatT}/
 * {@code MatF} NBT keys, matching {@code ItemCustomDrawers}.{@code makeItemStack}).
 *
 * <p>
 * The materials are part of the tile's "portable" state, so they travel with a sealed tank on
 * break and are restored on place via {@link ItemBlockTankCustom}. Because
 * {@link TileTank#writeToNBT}/{@link TileTank#readFromNBT} route through the (virtual)
 * {@code writeToPortableNBT}/{@code readFromPortableNBT} pair, overriding those here is enough to
 * persist the materials both to disk and to a sealed item.
 */
public class TileTankCustom extends TileTank {

    private ItemStack matSide;
    private ItemStack matTrim;
    private ItemStack matFront;

    public ItemStack getMaterialSide() {
        return matSide;
    }

    public void setMaterialSide(ItemStack stack) {
        this.matSide = normalize(stack);
    }

    public ItemStack getMaterialTrim() {
        return matTrim;
    }

    public void setMaterialTrim(ItemStack stack) {
        this.matTrim = normalize(stack);
    }

    public ItemStack getMaterialFront() {
        return matFront;
    }

    public void setMaterialFront(ItemStack stack) {
        this.matFront = normalize(stack);
    }

    /** True if at least one material is set (used to decide raw vs framed rendering). */
    public boolean hasMaterials() {
        return matSide != null || matTrim != null || matFront != null;
    }

    private static ItemStack normalize(ItemStack stack) {
        if (stack == null) return null;
        ItemStack copy = stack.copy();
        copy.stackSize = 1;
        return copy;
    }

    @Override
    public void writeToPortableNBT(NBTTagCompound tag) {
        super.writeToPortableNBT(tag);
        if (matSide != null) tag.setTag("MatS", matSide.writeToNBT(new NBTTagCompound()));
        if (matTrim != null) tag.setTag("MatT", matTrim.writeToNBT(new NBTTagCompound()));
        if (matFront != null) tag.setTag("MatF", matFront.writeToNBT(new NBTTagCompound()));
    }

    @Override
    public void readFromPortableNBT(NBTTagCompound tag) {
        super.readFromPortableNBT(tag);
        this.matSide = tag.hasKey("MatS", 10) ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatS")) : null;
        this.matTrim = tag.hasKey("MatT", 10) ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatT")) : null;
        this.matFront = tag.hasKey("MatF", 10) ? ItemStack.loadItemStackFromNBT(tag.getCompoundTag("MatF")) : null;
    }
}
