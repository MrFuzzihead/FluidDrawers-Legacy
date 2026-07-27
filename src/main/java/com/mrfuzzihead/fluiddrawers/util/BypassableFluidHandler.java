package com.mrfuzzihead.fluiddrawers.util;

import javax.annotation.Nullable;

import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

public interface BypassableFluidHandler extends IFluidHandler {

    int fill(ForgeDirection from, FluidStack resource, boolean doFill, boolean bypass);

    @Nullable
    FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain, boolean bypass);

    @Nullable
    FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain, boolean bypass);

    @Override
    default int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return fill(from, resource, doFill, false);
    }

    @Override
    @Nullable
    default FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return drain(from, resource, doDrain, false);
    }

    @Override
    @Nullable
    default FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return drain(from, maxDrain, doDrain, false);
    }
}
