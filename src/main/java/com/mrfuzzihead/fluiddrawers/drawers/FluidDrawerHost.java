package com.mrfuzzihead.fluiddrawers.drawers;

import javax.annotation.Nullable;

import net.minecraftforge.fluids.FluidStack;

import com.mrfuzzihead.fluiddrawers.util.SimpleDrawerAttributes;

public interface FluidDrawerHost {

    FluidDrawerGroup getFluidDrawerGroup();

    int getUnmodifiedBaseCapacity();

    int getModifiedBaseCapacity();

    int getStorageMultiplier();

    default int getCapacity() {
        SimpleDrawerAttributes attrs = this.getAttributes();
        return !attrs.isUnlimitedStorage() && !attrs.isUnlimitedVending()
            ? this.getModifiedBaseCapacity() * this.getStorageMultiplier()
            : Integer.MAX_VALUE;
    }

    SimpleDrawerAttributes getAttributes();

    default boolean isFluidDrawerHostValid() {
        return true;
    }

    default void onStoredFluidChanged(int slot, @Nullable FluidStack oldFluid, @Nullable FluidStack newFluid) {}
}
