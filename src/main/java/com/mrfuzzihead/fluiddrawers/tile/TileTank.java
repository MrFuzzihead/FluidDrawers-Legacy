package com.mrfuzzihead.fluiddrawers.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/**
 * Plain {@link TileEntity} for the Fluid Tank block (no Chameleon dependency).
 *
 * Phase 3 stub: persists empty NBT across save/reload. The fluid stack, upgrades, attributes,
 * seal state, and ownership will be added in Phases 4+ as inline NBT fields (Chameleon data
 * shims are unavailable in 1.7.10 — SD 1.7.10 inlines everything in TileEntityDrawers).
 */
public class TileTank extends TileEntity {

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        // TODO: Phase 4 will persist the fluid stack, upgrades, attributes, sealed, owner etc.
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        // TODO: Phase 4 will restore the fluid stack, upgrades, attributes, sealed, owner etc.
    }
}
