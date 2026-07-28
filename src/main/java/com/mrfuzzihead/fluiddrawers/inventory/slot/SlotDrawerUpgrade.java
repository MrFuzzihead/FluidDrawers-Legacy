package com.mrfuzzihead.fluiddrawers.inventory.slot;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.fluiddrawers.tile.TileTank;

/**
 * Upgrade slot for the tank GUI. The slot accepts SD upgrade items and limits them to
 * one per slot. Validity checking (canAddUpgrade / canRemoveUpgrade) is delegated to
 * the backing {@link TileTank}.
 *
 * Ports the 1.12.2 {@code SlotDrawerUpgrade} which extended {@code SlotItemHandler}
 * (Forge capability inventory). In 1.7.10 we use a plain {@link Slot} backed by a
 * {@link TileTank} that implements upgrade validation.
 */
public class SlotDrawerUpgrade extends Slot {

    private final TileTank tile;

    public SlotDrawerUpgrade(TileTank tile, int index, int posX, int posY) {
        super(tile.getUpgradeInventory(), index, posX, posY);
        this.tile = tile;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return stack != null && this.tile.getUpgradeInventory()
            .isItemValidForSlot(this.getSlotIndex(), stack);
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        // Phase 9: check canRemoveUpgrade to prevent removing upgrades
        // when current fluid exceeds the resulting capacity.
        return this.tile.canRemoveUpgrade(this.getSlotIndex());
    }

    @Override
    public void onSlotChanged() {
        super.onSlotChanged();
        this.tile.markDirty();
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
