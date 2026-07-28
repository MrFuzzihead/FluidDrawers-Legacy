package com.mrfuzzihead.fluiddrawers.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
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

    /**
     * The light level the tank block should emit, based on the stored fluid's luminosity. Unscaled
     * by fill level -- matches the OpenBlocks Tank reference (which returns the fluid's luminosity
     * directly); the 1.12.2 FluidDrawers source does not override getLightValue at all, so
     * OpenBlocks is the authoritative reference here. Returns 0 when the tank is empty (no fluid
     * set) or the stored fluid has no luminosity.
     */
    public int getFluidLightLevel() {
        FluidStack fluid = drawerGroup.getFluidDrawer()
            .getStoredFluid();
        return fluid != null ? fluid.getFluid()
            .getLuminosity(fluid) : 0;
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
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            // If the fluid's luminosity changed, re-run the lighting engine at this position so a
            // luminous fluid (e.g. lava) makes the tank emit light and draining it removes that
            // light. markBlockForUpdate only notifies render accesses; it does NOT recalculate
            // light. func_147451_t runs updateLightByType for both Sky and Block (World:3268).
            // Matches the OpenBlocks TileEntityTank pattern: only relight when luminosity actually
            // changes (avoids relight churn on every water bucket).
            int oldLum = oldFluid != null ? oldFluid.getFluid()
                .getLuminosity(oldFluid) : 0;
            int newLum = newFluid != null ? newFluid.getFluid()
                .getLuminosity(newFluid) : 0;
            if (oldLum != newLum) {
                worldObj.func_147451_t(xCoord, yCoord, zCoord);
            }
        }
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

    // --- Sync (description packet) ---

    @Override
    public Packet getDescriptionPacket() {
        NBTTagCompound tag = new NBTTagCompound();
        writeToNBT(tag);
        return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 5, tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt) {
        readFromNBT(pkt.func_148857_g());
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    // --- Render pass selection (Phase 7 TESR) ---

    // Render the fluid TESR in the solid pass (0), behind the alpha-pass glass frame drawn by
    // BlockTankRenderer (pass 1). TileEntity's default already returns pass == 0; this override
    // makes the intent explicit and gives future phases (e.g. Phase 11 overlays) a clear place to
    // opt into additional passes. The base method is not @SideOnly, so this is server-safe.
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0;
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
