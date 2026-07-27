package com.mrfuzzihead.fluiddrawers.drawers;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.IFluidHandler;

import com.mrfuzzihead.fluiddrawers.util.DrawerFluidHandler;

public class SingletonFluidDrawerGroup implements FluidDrawerGroup {

    private static final int[] SINGLETON_SLOT = new int[] { 0 };

    private final FluidDrawerHost host;
    private final SimpleFluidDrawer fluidDrawer;
    private final DrawerFluidHandler fluidHandler;

    public SingletonFluidDrawerGroup(FluidDrawerHost host) {
        this.host = host;
        this.fluidDrawer = new SimpleFluidDrawer(0, host);
        this.fluidHandler = new DrawerFluidHandler(this);
    }

    public FluidDrawer getFluidDrawer() {
        return this.fluidDrawer;
    }

    @Override
    public int getFluidDrawerCount() {
        return 1;
    }

    @Override
    public FluidDrawer getFluidDrawer(int slot) {
        if (slot != 0) {
            throw new IndexOutOfBoundsException("Singleton fluid drawer group only has slot 0, but got: " + slot);
        }
        return this.getFluidDrawer();
    }

    @Override
    public int[] getAccessibleFluidDrawerSlots() {
        return SINGLETON_SLOT;
    }

    @Override
    public boolean isFluidDrawerGroupValid() {
        return this.host.isFluidDrawerHostValid();
    }

    public IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag.setTag("Drawer", this.fluidDrawer.serializeNBT());
        return tag;
    }

    public void readFromNBT(NBTTagCompound tag) {
        if (tag.hasKey("Drawer")) {
            this.fluidDrawer.deserializeNBT(tag.getCompoundTag("Drawer"));
        }
    }
}
