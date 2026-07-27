package com.mrfuzzihead.fluiddrawers.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.FluidStack;

import com.mrfuzzihead.fluiddrawers.Config;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerGroup;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerHost;
import com.mrfuzzihead.fluiddrawers.drawers.SingletonFluidDrawerGroup;
import com.mrfuzzihead.fluiddrawers.util.SimpleDrawerAttributes;

/**
 * Tile entity for the Fluid Tank. Holds a {@link SingletonFluidDrawerGroup} that wraps a single
 * {@link com.mrfuzzihead.fluiddrawers.drawers.SimpleFluidDrawer} and exposes a 1.7.10
 * {@link net.minecraftforge.fluids.IFluidHandler} via the group.
 *
 * Phase 4: holds the fluid drawer group, persists the fluid stack, exposes capacity from config.
 * The attributes/upgrades/seal/owner fields come in later phases.
 */
public class TileTank extends TileEntity implements FluidDrawerHost {

    private final SingletonFluidDrawerGroup drawerGroup;
    private final TankAttributes attributes;

    public TileTank() {
        this.attributes = new TankAttributes();
        this.drawerGroup = new SingletonFluidDrawerGroup(this);
    }

    public SingletonFluidDrawerGroup getDrawerGroup() {
        return drawerGroup;
    }

    // --- FluidDrawerHost ---

    @Override
    public FluidDrawerGroup getFluidDrawerGroup() {
        return drawerGroup;
    }

    @Override
    public int getUnmodifiedBaseCapacity() {
        return Config.baseCapacity;
    }

    @Override
    public int getModifiedBaseCapacity() {
        return getUnmodifiedBaseCapacity();
    }

    @Override
    public int getStorageMultiplier() {
        return 1;
    }

    @Override
    public SimpleDrawerAttributes getAttributes() {
        return attributes;
    }

    @Override
    public void onStoredFluidChanged(int slot, FluidStack oldFluid, FluidStack newFluid) {
        markDirty();
    }

    @Override
    public boolean isFluidDrawerHostValid() {
        return !isInvalid();
    }

    // --- NBT persistance ---

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        this.drawerGroup.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.drawerGroup.readFromNBT(tag);
    }

    // --- inner: TankAttributes ---

    private class TankAttributes extends SimpleDrawerAttributes {

        private TankAttributes() {}

        @Override
        protected void onAttributeChanged() {
            markDirty();
        }
    }
}
