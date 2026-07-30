package com.mrfuzzihead.fluiddrawers.drawers;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

public interface FluidDrawer {

    @Nullable
    FluidStack getStoredFluid();

    FluidDrawer setStoredFluid(@Nullable FluidStack fluid);

    default FluidDrawer setStoredFluidPrototype(@Nullable FluidStack fluid) {
        if (fluid == null) {
            return this.setStoredFluid(null);
        } else {
            FluidStack prototype = fluid.copy();
            prototype.amount = 0;
            return this.setStoredFluid(prototype);
        }
    }

    default int insertFluid(@Nullable FluidStack fluid, boolean commit, boolean bypass) {
        if (fluid != null && fluid.amount > 0 && (bypass || this.canFluidBeStored(fluid))) {
            FluidStack stored = this.getStoredFluid();
            if (stored != null && !stored.isFluidEqual(fluid)) {
                return 0;
            } else {
                int toTransfer = Math.min(this.getAcceptingRemainingCapacity(), fluid.amount);
                if (commit && toTransfer > 0) {
                    if (stored != null) {
                        stored = stored.copy();
                        if (stored.amount > Integer.MAX_VALUE - toTransfer) {
                            stored.amount = Integer.MAX_VALUE;
                        } else {
                            stored.amount += toTransfer;
                        }
                    } else {
                        stored = fluid.copy();
                        stored.amount = toTransfer;
                    }

                    this.setStoredFluid(stored);
                }

                return toTransfer;
            }
        } else {
            return 0;
        }
    }

    @Nullable
    default FluidStack extractFluid(@Nullable FluidStack fluid, boolean commit, boolean bypass) {
        if (fluid != null && fluid.amount > 0 && (bypass || this.canFluidBeExtracted(fluid))) {
            FluidStack stored = this.getStoredFluid();
            if (stored != null && stored.isFluidEqual(fluid)) {
                int toTransfer = Math.min(stored.amount, fluid.amount);
                if (commit && toTransfer > 0) {
                    stored = stored.copy();
                    stored.amount -= toTransfer;
                    this.setStoredFluid(stored);
                }

                fluid = stored.copy();
                fluid.amount = toTransfer;
                return fluid;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Nullable
    default FluidStack extractFluid(int amount, boolean commit, boolean bypass) {
        if (amount <= 0) {
            return null;
        } else {
            FluidStack fluid = this.getStoredFluid();
            if (fluid == null) {
                return null;
            } else {
                fluid = fluid.copy();
                fluid.amount = amount;
                return this.extractFluid(fluid, commit, bypass);
            }
        }
    }

    default int getMaxCapacity() {
        return this.getMaxCapacity(this.getStoredFluid());
    }

    int getMaxCapacity(@Nullable FluidStack fluid);

    default int getAcceptingMaxCapacity(@Nullable FluidStack fluid) {
        return this.getMaxCapacity(fluid);
    }

    int getRemainingCapacity();

    default int getAcceptingRemainingCapacity() {
        return this.getRemainingCapacity();
    }

    boolean canFluidBeStored(@Nullable FluidStack fluid);

    boolean canFluidBeExtracted(@Nullable FluidStack fluid);

    default boolean isEmpty() {
        FluidStack fluid = this.getStoredFluid();
        return fluid == null || fluid.amount <= 0;
    }
}
