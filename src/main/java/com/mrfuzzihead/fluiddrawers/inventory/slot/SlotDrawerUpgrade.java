package com.mrfuzzihead.fluiddrawers.inventory.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Upgrade slot for the tank GUI. The slot accepts SD upgrade items and limits them to
 * one per slot. Validity checking (canAddUpgrade / canRemoveUpgrade) is delegated to the
 * backing {@link IInventory}, which is inert (no-op) until Phase 9 wires up the upgrade logic.
 *
 * Ports the 1.12.2 {@code SlotDrawerUpgrade} which extended {@code SlotItemHandler}
 * (Forge capability inventory). In 1.7.10 we use a plain {@link Slot} backed by an
 * {@link IInventory} on the tile entity (the "upgrade slots").
 */
public class SlotDrawerUpgrade extends Slot {

    private final IInventory inv;

    public SlotDrawerUpgrade(IInventory inv, int index, int posX, int posY) {
        super(inv, index, posX, posY);
        this.inv = inv;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack != null && this.inv.isItemValidForSlot(this.getSlotIndex(), stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        // Phase 9: also check this.inv.canRemoveUpgrade(this.getSlotIndex())
        return true;
    }

    @Override
    public void onSlotChanged() {
        super.onSlotChanged();
        this.inv.markDirty();
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
