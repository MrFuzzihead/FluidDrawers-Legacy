package com.mrfuzzihead.fluiddrawers.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer;
import com.mrfuzzihead.fluiddrawers.inventory.slot.SlotDrawerUpgrade;
import com.mrfuzzihead.fluiddrawers.tile.TileTank;

/**
 * Container for the Fluid Tank GUI. Ports the 1.12.2 {@code ContainerTank} which extended
 * libnine's {@code L9Container} (itself a subclass of {@code Container} with an embedded
 * player-inventory loop). In the 1.7.10 port we extend {@link Container} directly and
 * manually add the player inventory/hotbar slots, matching the vanilla 1.7.10 pattern.
 *
 * <p>
 * Layout (matching 1.12.2 FD):
 * <ul>
 * <li>7 upgrade slots at y=86, x offset 26 + i*18 (i = 0..6)</li>
 * <li>Player inventory (3 rows × 9 col) below the upgrade area</li>
 * <li>Hotbar (1 row × 9 col) at the bottom</li>
 * </ul>
 * GUI height is 199 (port of 1.12.2 199-pixel-tall frame).
 */
public class ContainerTank extends Container {

    private final TileTank tile;

    public ContainerTank(InventoryPlayer playerInv, TileTank tile) {
        this.tile = tile;

        // 7 upgrade slots (wired up in Phase 9)
        for (int i = 0; i < 7; i++) {
            this.addSlotToContainer(new SlotDrawerUpgrade(tile, i, 26 + i * 18, 86));
        }

        // Player inventory (3 rows × 9 col, starting at y=117)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlotToContainer(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 117 + row * 18));
            }
        }

        // Hotbar (1 row × 9 col, starting at y=175)
        for (int col = 0; col < 9; col++) {
            this.addSlotToContainer(new Slot(playerInv, col, 8 + col * 18, 175));
        }
    }

    /**
     * Exposes the single fluid drawer to the GUI widget for rendering the fluid level.
     */
    public FluidDrawer getFluidDrawer() {
        return this.tile.getDrawerGroup()
            .getFluidDrawer();
    }

    public TileTank getTileTank() {
        return this.tile;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return this.tile.getDistanceFrom(player.posX, player.posY, player.posZ) <= 64.0D;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        Slot slot = (Slot) this.inventorySlots.get(slotIndex);
        if (slot == null || !slot.getHasStack()) return null;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        // Upgrade slots (0..6) -> player inventory
        if (slotIndex < 7) {
            if (!this.mergeItemStack(stack, 7, 43, true)) return null;
            slot.onSlotChange(stack, original);
        }
        // Player inventory or hotbar (7..42) -> upgrade slots
        else {
            // Try to place into upgrade slots (0..6) first; if full, shift within player inv
            if (!this.mergeItemStack(stack, 0, 7, false)) {
                if (slotIndex < 34) {
                    // Player inventory (7..33) -> hotbar (34..42)
                    if (!this.mergeItemStack(stack, 34, 43, false)) return null;
                } else {
                    // Hotbar (34..42) -> player inventory (7..33)
                    if (!this.mergeItemStack(stack, 7, 34, false)) return null;
                }
            }
        }

        if (stack.stackSize == 0) {
            slot.putStack(null);
        } else {
            slot.onSlotChanged();
        }

        slot.onPickupFromSlot(player, stack);
        return original;
    }
}
