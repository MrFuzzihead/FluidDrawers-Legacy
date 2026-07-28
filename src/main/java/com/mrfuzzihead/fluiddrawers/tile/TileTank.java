package com.mrfuzzihead.fluiddrawers.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import com.mrfuzzihead.fluiddrawers.Config;
import com.mrfuzzihead.fluiddrawers.drawers.DrawerUpgradable;
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
    private final UpgradeInventory upgradeInventory;
    private final TankUpgradeData upgradeData;

    public static final int UPGRADE_SLOT_COUNT = 7;

    public TileTank() {
        this.attributes = new TankAttributes();
        this.drawerGroup = new SingletonFluidDrawerGroup(this);
        this.upgradeInventory = new UpgradeInventory();
        this.upgradeData = new TankUpgradeData();
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

    /**
     * Returns the base capacity, or the downgraded base capacity if a downgrade
     * is installed. Does NOT multiply by the storage multiplier — that is applied
     * by {@link FluidDrawerHost#getCapacity()}. Matches the 1.12.2 FD contract
     * where {@code getCapacity() = getModifiedBaseCapacity() * getStorageMultiplier()}.
     */
    @Override
    public int getModifiedBaseCapacity() {
        return upgradeData.isDowngraded() ? Config.baseCapacityDowngraded : getUnmodifiedBaseCapacity();
    }

    /**
     * Returns the downgraded base capacity (from config).
     * Does NOT multiply by the storage multiplier — that is applied by
     * {@link FluidDrawerHost#getCapacity()}.
     */
    public int getDowngradedBaseCapacity() {
        return Config.baseCapacityDowngraded;
    }

    /**
     * Phase 9: Sum the storage upgrade multipliers from all installed storage
     * upgrades ({@code ItemUpgrade} with metadata >= 2). Returns at least 1.
     */
    @Override
    public int getStorageMultiplier() {
        int multiplier = 0;
        for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
            ItemStack upgrade = upgradeData.getUpgrade(i);
            if (upgrade != null && DrawerUpgradable.isStorageUpgrade(upgrade)) {
                multiplier += DrawerUpgradable.getStorageMultiplier(upgrade);
            }
        }
        return Math.max(multiplier, 1);
    }

    @Override
    public SimpleDrawerAttributes getAttributes() {
        return attributes;
    }

    // --- Upgrade accessors ---

    /**
     * Returns true if a downgrade upgrade is installed.
     */
    public boolean isDowngraded() {
        return upgradeData.isDowngraded();
    }

    /**
     * Check if a storage upgrade can be removed from the given slot without
     * dropping the fluid below the new (lower) capacity.
     */
    public boolean canRemoveUpgrade(int slot) {
        return upgradeData.canRemoveUpgrade(slot);
    }

    /**
     * Whether a redstone-level emitter upgrade is installed.
     */
    public boolean hasLevelEmitter() {
        return upgradeData.hasLevelEmitter();
    }

    /**
     * The current redstone signal strength (1-15) proportional to fill level.
     * Returns 0 when empty.
     */
    public int getRedstoneLevel() {
        if (!hasLevelEmitter()) {
            return 0;
        }
        FluidStack fluid = drawerGroup.getFluidDrawer()
            .getStoredFluid();
        if (fluid == null || fluid.amount <= 0) {
            return 0;
        }
        int capacity = drawerGroup.getFluidDrawer()
            .getMaxCapacity();
        if (capacity <= 0) {
            return 0;
        }
        return MathHelper.clamp_int(1 + (int) (14 * fluid.amount / capacity), 1, 15);
    }

    /**
     * Whether the tank has a creative vending upgrade installed.
     */
    public boolean isVending() {
        return attributes.isUnlimitedVending();
    }

    /**
     * Notify the world that this block's state has changed.
     */
    public void notifyBlockUpdate() {
        if (worldObj != null) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
        }
    }

    /**
     * Notify adjacent blocks of a neighbor change (for redstone upgrade).
     */
    public void notifyNeighbors() {
        if (worldObj != null) {
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, blockType);
            worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord - 1, zCoord, blockType);
        }
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
            // Phase 10: notify neighbors when redstone upgrade is installed so
            // the redstone signal updates immediately on fill/drain.
            if (hasLevelEmitter()) {
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, blockType);
                worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord - 1, zCoord, blockType);
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
        tag.setTag("Attributes", this.attributes.serializeNBT());
        this.upgradeData.writeToNBT(tag);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.drawerGroup.readFromNBT(tag);
        this.attributes.deserializeNBT(tag.getCompoundTag("Attributes"));
        this.upgradeData.readFromNBT(tag);
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

    /**
     * Exposes the upgrade IInventory (7 slots, wired up in Phase 9).
     */
    public IInventory getUpgradeInventory() {
        return this.upgradeInventory;
    }

    // --- inner: TankUpgradeData ---

    /**
     * Upgrade data manager for the tank. Tracks installed upgrades and validates
     * add/remove operations. Ports the 1.12.2
     * {@code TileTank.TankUpgradeData}.
     */
    private class TankUpgradeData {

        private final ItemStack[] upgradeSlots = new ItemStack[UPGRADE_SLOT_COUNT];
        private boolean downgraded = false;
        private boolean redstoneEmitter = false;

        /**
         * Phase 10: Apply upgrade effects to the tank attributes by scanning
         * all upgrade slots. Void, lock, creative/vending, and redstone upgrades
         * set the corresponding attribute flags. Called after every change to
         * the upgrade slots (setUpgrade, clearUpgrade, readFromNBT).
         */
        private void applyUpgradeEffects() {
            boolean hasVoid = false;
            boolean hasVending = false;
            boolean hasRedstone = false;

            for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                ItemStack stack = upgradeSlots[i];
                if (stack == null) continue;
                if (stack.getItem() instanceof com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeVoid) {
                    hasVoid = true;
                }
                if (stack.getItem() instanceof com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeCreative) {
                    if (stack.getItemDamage() == 1) {
                        hasVending = true;
                    }
                }
                if (stack.getItem() instanceof com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeRedstone) {
                    hasRedstone = true;
                }
            }

            attributes.setVoid(hasVoid);
            // Lock is handled via onBlockActivated interaction (Phase 11), not upgrade slots
            attributes.setUnlimitedVending(hasVending);
            redstoneEmitter = hasRedstone;

            if (worldObj != null && !worldObj.isRemote) {
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                if (hasRedstone) {
                    notifyNeighbors();
                }
            }
        }

        public boolean hasLevelEmitter() {
            return redstoneEmitter;
        }

        public boolean canAddUpgrade(ItemStack upgrade) {
            if (!DrawerUpgradable.isUpgradeItem(upgrade)) {
                return false;
            }
            if (upgrade.getItem() instanceof com.jaquadro.minecraft.storagedrawers.item.ItemUpgradeStatus) {
                return false;
            }
            if (DrawerUpgradable.isStorageUpgrade(upgrade)) {
                int newMult = getStorageMultiplier() + DrawerUpgradable.getStorageMultiplier(upgrade);
                return isCapacityAcceptable(getUnmodifiedBaseCapacity() * newMult);
            }
            return true;
        }

        public boolean canRemoveUpgrade(int slot) {
            ItemStack upgrade = slot >= 0 && slot < UPGRADE_SLOT_COUNT ? upgradeSlots[slot] : null;
            if (upgrade == null) {
                return true;
            }
            if (DrawerUpgradable.isStorageUpgrade(upgrade)) {
                int thisMult = DrawerUpgradable.getStorageMultiplier(upgrade);
                int remainingMult = Math.max(getStorageMultiplier() - thisMult, 1);
                int newCapacity = getUnmodifiedBaseCapacity() * remainingMult;
                return isCapacityAcceptable(newCapacity);
            }
            return true;
        }

        private boolean isCapacityAcceptable(int newCapacity) {
            FluidStack fluid = drawerGroup.getFluidDrawer()
                .getStoredFluid();
            return fluid == null || fluid.amount <= newCapacity;
        }

        public void setUpgrade(int slot, ItemStack stack) {
            if (slot >= 0 && slot < UPGRADE_SLOT_COUNT) {
                if (stack != null) {
                    stack = stack.copy();
                    stack.stackSize = 1;
                }
                upgradeSlots[slot] = stack;
            }
            updateDowngradeState();
            applyUpgradeEffects();
            notifyBlockUpdate();
        }

        public void clearUpgrade(int slot) {
            if (slot >= 0 && slot < UPGRADE_SLOT_COUNT) {
                upgradeSlots[slot] = null;
            }
            updateDowngradeState();
            applyUpgradeEffects();
            notifyBlockUpdate();
        }

        public ItemStack getUpgrade(int slot) {
            if (slot < 0 || slot >= UPGRADE_SLOT_COUNT) {
                return null;
            }
            return upgradeSlots[slot];
        }

        public boolean isDowngraded() {
            return downgraded;
        }

        private void updateDowngradeState() {
            boolean wasDowngraded = downgraded;
            downgraded = false;
            for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                ItemStack upgrade = upgradeSlots[i];
                if (upgrade != null && DrawerUpgradable.isDowngrade(upgrade)) {
                    downgraded = true;
                    break;
                }
            }
            if (downgraded != wasDowngraded) {
                notifyBlockUpdate();
            }
        }

        public void writeToNBT(NBTTagCompound tag) {
            NBTTagCompound upgradesTag = new NBTTagCompound();
            for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                ItemStack stack = upgradeSlots[i];
                if (stack != null) {
                    NBTTagCompound slotTag = new NBTTagCompound();
                    stack.writeToNBT(slotTag);
                    upgradesTag.setTag("Upgrade" + i, slotTag);
                }
            }
            tag.setTag("Upgrades", upgradesTag);
            tag.setBoolean("Downgraded", downgraded);
        }

        public void readFromNBT(NBTTagCompound tag) {
            for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                upgradeSlots[i] = null;
            }
            if (tag.hasKey("Upgrades")) {
                NBTTagCompound upgradesTag = tag.getCompoundTag("Upgrades");
                for (int i = 0; i < UPGRADE_SLOT_COUNT; i++) {
                    String key = "Upgrade" + i;
                    if (upgradesTag.hasKey(key)) {
                        NBTTagCompound slotTag = upgradesTag.getCompoundTag(key);
                        upgradeSlots[i] = ItemStack.loadItemStackFromNBT(slotTag);
                    }
                }
            }
            downgraded = tag.getBoolean("Downgraded");
            applyUpgradeEffects();
        }
    }

    // --- inner: UpgradeInventory ---

    /**
     * Inventory for the 7 upgrade slots, backed by {@link TankUpgradeData}.
     * Phase 9 wires up {@link #isItemValidForSlot} via
     * {@link TankUpgradeData#canAddUpgrade}.
     */
    private class UpgradeInventory implements IInventory {

        @Override
        public int getSizeInventory() {
            return UPGRADE_SLOT_COUNT;
        }

        @Override
        public ItemStack getStackInSlot(int index) {
            return upgradeData.getUpgrade(index);
        }

        @Override
        public ItemStack decrStackSize(int index, int count) {
            ItemStack stack = upgradeData.getUpgrade(index);
            if (stack != null) {
                upgradeData.clearUpgrade(index);
            }
            return stack;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int index) {
            ItemStack stack = upgradeData.getUpgrade(index);
            if (stack != null) {
                upgradeData.clearUpgrade(index);
            }
            return stack;
        }

        @Override
        public void setInventorySlotContents(int index, ItemStack stack) {
            upgradeData.setUpgrade(index, stack);
        }

        @Override
        public String getInventoryName() {
            return "fluiddrawers.upgrades";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 1;
        }

        @Override
        public void markDirty() {
            TileTank.this.markDirty();
        }

        @Override
        public boolean isUseableByPlayer(EntityPlayer player) {
            return TileTank.this.getDistanceFrom(player.posX, player.posY, player.posZ) <= 64.0D;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return upgradeData.canAddUpgrade(stack);
        }

        public void onInventoryChanged() {
            TileTank.this.markDirty();
        }
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
