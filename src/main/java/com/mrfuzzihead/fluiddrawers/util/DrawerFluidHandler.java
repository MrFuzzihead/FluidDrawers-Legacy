package com.mrfuzzihead.fluiddrawers.util;

import javax.annotation.Nullable;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerGroup;

public class DrawerFluidHandler implements IFluidHandler {

    private final FluidDrawerGroup group;

    public DrawerFluidHandler(FluidDrawerGroup group) {
        this.group = group;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) {
            return 0;
        }
        int origAmount = resource.amount;
        resource = resource.copy();

        for (int slotIndex : this.group.getAccessibleFluidDrawerSlots()) {
            resource.amount -= this.group.getFluidDrawer(slotIndex)
                .insertFluid(resource, doFill, false);
            if (resource.amount <= 0) {
                return origAmount;
            }
        }
        return origAmount - resource.amount;
    }

    @Nullable
    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) {
            return null;
        }
        int origAmount = resource.amount;
        resource = resource.copy();

        for (int slotIndex : this.group.getAccessibleFluidDrawerSlots()) {
            FluidStack extracted = this.group.getFluidDrawer(slotIndex)
                .extractFluid(resource, doDrain, false);
            if (extracted != null && extracted.amount > 0) {
                resource.amount -= extracted.amount;
                if (resource.amount <= 0) {
                    resource.amount = origAmount;
                    return resource;
                }
            }
        }
        resource.amount = origAmount - resource.amount;
        return resource;
    }

    @Nullable
    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) {
            return null;
        }
        for (int slotIndex : this.group.getAccessibleFluidDrawerSlots()) {
            FluidStack fluid = this.group.getFluidDrawer(slotIndex)
                .extractFluid(maxDrain, false, false);
            if (fluid != null && fluid.amount > 0) {
                FluidStack request = fluid.copy();
                request.amount = maxDrain;
                return this.drain(from, request, doDrain);
            }
        }
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        for (int slotIndex : this.group.getAccessibleFluidDrawerSlots()) {
            FluidDrawer drawer = this.group.getFluidDrawer(slotIndex);
            FluidStack stored = drawer.getStoredFluid();
            if (stored == null) {
                return true;
            }
            if (stored.getFluid() == fluid && drawer.canFluidBeStored(stored)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        for (int slotIndex : this.group.getAccessibleFluidDrawerSlots()) {
            FluidStack stored = this.group.getFluidDrawer(slotIndex)
                .getStoredFluid();
            if (stored != null && stored.getFluid() == fluid && stored.amount > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        int[] slots = this.group.getAccessibleFluidDrawerSlots();
        FluidTankInfo[] tankInfo = new FluidTankInfo[slots.length];
        for (int i = 0; i < slots.length; i++) {
            FluidDrawer drawer = this.group.getFluidDrawer(slots[i]);
            FluidStack fluid = drawer.getStoredFluid();
            tankInfo[i] = new FluidTankInfo(fluid != null ? fluid.copy() : null, drawer.getAcceptingMaxCapacity(fluid));
        }
        return tankInfo;
    }
}
