package com.mrfuzzihead.fluiddrawers.drawers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

/**
 * Represents a single fluid storage slot within a tank drawer.
 * Ported from 1.12 FluidDrawer implementation.
 */
public class FluidDrawer implements IFluidTank {

    public static final int MAX_FLUID = 16000; // mB - default capacity for a single drawer

    private FluidStack fluid;

    public FluidDrawer() {
        this.fluid = null;
    }

    public FluidDrawer(FluidStack stack) {
        this.fluid = stack != null ? stack.copy() : null;
    }

    /**
     * Returns the current fluid stored in this drawer.
     */
    @Override
    public FluidStack getFluid() {
        return fluid;
    }

    /**
     * Sets the fluid for this drawer, copying the input to prevent external modification.
     */
    public void setFluid(FluidStack stack) {
        if (stack != null) {
            this.fluid = stack.copy();
        } else {
            this.fluid = null;
        }
    }

    /**
     * Returns the maximum capacity of this drawer in mB.
     */
    @Override
    public int getCapacity() {
        return MAX_FLUID;
    }

    /**
     * Returns the amount of fluid stored in mB.
     */
    @Override
    public int getFluidAmount() {
        if (fluid == null) {
            return 0;
        }
        return fluid.amount;
    }

    /**
     * Drains up to maxDrain mB of the stored fluid, returning a new FluidStack.
     */
    @Override
    public FluidStack drain(int maxDrain, boolean doDrain) {
        if (fluid == null || fluid.amount <= 0) {
            return null;
        }

        int toDrain = Math.min(maxDrain, fluid.amount);
        if (!doDrain) {
            return new FluidStack(fluid.getFluid(), toDrain);
        }

        // Actually drain the fluid
        fluid.amount -= toDrain;
        if (fluid.amount <= 0) {
            this.fluid = null;
        }
        return new FluidStack(fluid.getFluid(), toDrain);
    }

    /**
     * Fills this drawer with up to maxFill mB of the given fluid.
     */
    @Override
    public int fill(FluidStack resource, boolean doFill) {
        if (resource == null || !canFillFluidType(resource)) {
            return 0;
        }

        // If we have no fluid yet, accept any compatible type
        // If we have fluid, only accept matching types
        int toFill = Math.min(maxFill() - getFluidAmount(), resource.amount);
        if (toFill <= 0) {
            return 0;
        }

        if (!doFill) {
            return toFill;
        }

        // Actually fill the fluid
        if (fluid == null) {
            this.fluid = new FluidStack(resource.getFluid(), toFill);
        } else {
            fluid.amount += toFill;
        }

        return toFill;
    }

    /**
     * Returns true if we can fill with the given fluid type.
     */
    public boolean canFillFluidType(FluidStack resource) {
        if (resource == null || !resource.isFluidStackValid()) {
            return false;
        }

        // If no existing fluid, accept anything valid
        if (fluid == null) {
            return true;
        }

        // Must match the existing fluid type
        return fluid.getFluid() == resource.getFluid();
    }

    /**
     * Returns remaining capacity in mB.
     */
    private int maxFill() {
        return getCapacity();
    }

    /**
     * Checks if this drawer has any fluid stored.
     */
    public boolean isEmpty() {
        return fluid == null || fluid.amount <= 0;
    }

    /**
     * Returns a deep copy of this FluidDrawer's data as NBT.
     */
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        if (fluid != null && !isEmpty()) {
            nbt.setTag("Fluid", fluid.writeToNBT(new NBTTagCompound()));
        }
        return nbt;
    }

    /**
     * Loads drawer data from NBT.
     */
    public void readFromNBT(NBTTagCompound nbt) {
        if (nbt.hasKey("Fluid")) {
            FluidStack stack = FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("Fluid"));
            setFluid(stack);
        } else {
            setFluid(null);
        }
    }

    /**
     * Creates a copy of this drawer.
     */
    public FluidDrawer copy() {
        return new FluidDrawer(fluid);
    }

    @Override
    public String getTankName() {
        return "fluid_drawer";
    }
}
