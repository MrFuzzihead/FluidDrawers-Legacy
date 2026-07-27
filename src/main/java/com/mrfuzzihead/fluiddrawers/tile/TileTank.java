package com.mrfuzzihead.fluiddrawers.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.mrfuzzihead.fluiddrawers.Config;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerGroup;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerHost;
import com.mrfuzzihead.fluiddrawers.drawers.SingletonFluidDrawerGroup;
import com.mrfuzzihead.fluiddrawers.util.SimpleDrawerAttributes;

/**
 * Tile entity for the Fluid Tank. Acts as the 1.7.10 {@link IFluidHandler} for bucket
 * interaction, delegating to the internal {@link SingletonFluidDrawerGroup}.
 */
public class TileTank extends TileEntity implements FluidDrawerHost, IFluidHandler {

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

    // --- IFluidHandler ---

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return drawerGroup.getFluidHandler()
            .fill(from, resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return drawerGroup.getFluidHandler()
            .drain(from, resource, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return drawerGroup.getFluidHandler()
            .drain(from, maxDrain, doDrain);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return drawerGroup.getFluidHandler()
            .canFill(from, fluid);
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return drawerGroup.getFluidHandler()
            .canDrain(from, fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return drawerGroup.getFluidHandler()
            .getTankInfo(from);
    }

    // --- NBT persistence ---

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
