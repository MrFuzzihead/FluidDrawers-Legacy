package com.mrfuzzihead.fluiddrawers.util;

import javax.annotation.Nullable;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class BypassingFluidHandlerWrapper implements IFluidHandler {

    private final BypassableFluidHandler delegate;

    public BypassingFluidHandlerWrapper(BypassableFluidHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return delegate.fill(from, resource, doFill, true);
    }

    @Override
    @Nullable
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return delegate.drain(from, resource, doDrain, true);
    }

    @Override
    @Nullable
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return delegate.drain(from, maxDrain, doDrain, true);
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return delegate.canFill(from, fluid);
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return delegate.canDrain(from, fluid);
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        return delegate.getTankInfo(from);
    }
}
