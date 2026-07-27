package com.mrfuzzihead.fluiddrawers.drawers;

import javax.annotation.Nullable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fluids.FluidStack;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;

public class SimpleFluidDrawer implements FluidDrawer {

    private final int slot;
    private final FluidDrawerHost host;
    @Nullable
    private FluidStack fluid = null;

    public SimpleFluidDrawer(int slot, FluidDrawerHost host) {
        this.slot = slot;
        this.host = host;
    }

    @Nullable
    @Override
    public FluidStack getStoredFluid() {
        if (this.fluid != null && this.host.getAttributes()
            .isUnlimitedVending()) {
            this.fluid.amount = Integer.MAX_VALUE;
        }
        return this.fluid;
    }

    @Override
    public FluidDrawer setStoredFluid(@Nullable FluidStack fluid) {
        if (fluid != null && fluid.amount > 0) {
            if (this.host.getAttributes()
                .isUnlimitedVending()) {
                if (this.fluid == null || !fluid.isFluidEqual(this.fluid)) {
                    this.updateFluid(fluid, Integer.MAX_VALUE);
                }
            } else if (this.fluid != null && fluid.isFluidEqual(this.fluid)) {
                FluidStack oldFluid = this.fluid.copy();
                this.fluid.amount = MathHelper.clamp_int(fluid.amount, 0, this.getMaxCapacity());
                this.host.onStoredFluidChanged(this.slot, oldFluid, this.fluid);
            } else {
                this.updateFluid(fluid, Math.min(fluid.amount, this.getMaxCapacity()));
            }
        } else if (this.fluid != null && this.fluid.amount > 0) {
            FluidStack oldFluid;
            if (this.host.getAttributes()
                .isItemLocked(LockAttribute.LOCK_POPULATED)) {
                oldFluid = this.fluid.copy();
                this.fluid.amount = 0;
            } else {
                oldFluid = this.fluid;
                this.fluid = null;
            }
            this.host.onStoredFluidChanged(this.slot, oldFluid, this.fluid);
        }
        return this;
    }

    private void updateFluid(FluidStack newFluid, int amount) {
        FluidStack oldFluid = this.fluid;
        this.fluid = newFluid.copy();
        this.fluid.amount = MathHelper.clamp_int(amount, 0, this.getMaxCapacity());
        this.host.onStoredFluidChanged(this.slot, oldFluid, this.fluid);
    }

    @Override
    public int getMaxCapacity(@Nullable FluidStack fluid) {
        return this.host.getCapacity();
    }

    @Override
    public int getAcceptingMaxCapacity(@Nullable FluidStack fluid) {
        return !this.host.getAttributes()
            .isVoid()
            && !this.host.getAttributes()
                .isUnlimitedVending() ? this.getMaxCapacity(fluid) : Integer.MAX_VALUE;
    }

    @Override
    public int getRemainingCapacity() {
        FluidStack fluid = this.getStoredFluid();
        return fluid != null ? this.getMaxCapacity() - fluid.amount : this.getMaxCapacity();
    }

    @Override
    public int getAcceptingRemainingCapacity() {
        return !this.host.getAttributes()
            .isVoid()
            && !this.host.getAttributes()
                .isUnlimitedVending() ? this.getRemainingCapacity() : Integer.MAX_VALUE;
    }

    @Override
    public boolean canFluidBeStored(@Nullable FluidStack fluid) {
        if (fluid == null) {
            return true;
        } else {
            return this.fluid == null ? !this.host.getAttributes()
                .isItemLocked(LockAttribute.LOCK_EMPTY)
                : this.fluid.amount == 0 && !this.host.getAttributes()
                    .isItemLocked(LockAttribute.LOCK_POPULATED) || this.fluid.isFluidEqual(fluid);
        }
    }

    @Override
    public boolean canFluidBeExtracted(@Nullable FluidStack fluid) {
        return fluid == null || this.fluid != null && this.fluid.isFluidEqual(fluid);
    }

    public NBTTagCompound serializeNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        if (this.fluid != null) {
            NBTTagCompound fluidTag = new NBTTagCompound();
            this.fluid.writeToNBT(fluidTag);
            tag.setTag("Fluid", fluidTag);
        }
        return tag;
    }

    public void deserializeNBT(NBTTagCompound tag) {
        if (tag.hasKey("Fluid")) {
            this.fluid = FluidStack.loadFluidStackFromNBT(tag.getCompoundTag("Fluid"));
        }
    }
}
