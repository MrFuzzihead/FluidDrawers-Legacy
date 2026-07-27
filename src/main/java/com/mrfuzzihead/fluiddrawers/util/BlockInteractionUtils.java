package com.mrfuzzihead.fluiddrawers.util;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

public class BlockInteractionUtils {

    public static boolean transferFluid(IFluidHandler tank, EntityPlayer player, ItemStack heldItem,
        ForgeDirection face, boolean bypass) {
        if (heldItem == null) {
            return false;
        }
        if (bypass && tank instanceof BypassableFluidHandler) {
            tank = new BypassingFluidHandlerWrapper((BypassableFluidHandler) tank);
        }

        // Try to empty a filled container into the tank
        FluidStack containedFluid = FluidContainerRegistry.getFluidForFilledItem(heldItem);
        if (containedFluid != null) {
            int filled = tank.fill(face, containedFluid, true);
            if (filled > 0) {
                if (!player.capabilities.isCreativeMode) {
                    ItemStack emptyContainer = FluidContainerRegistry.drainFluidContainer(heldItem);
                    if (emptyContainer != null) {
                        deductHeldAndGiveItem(player, heldItem, emptyContainer);
                    }
                }
                return true;
            }
            return false;
        }

        // Try to fill an empty container from the tank
        if (FluidContainerRegistry.isEmptyContainer(heldItem)) {
            FluidStack drained = tank.drain(face, FluidContainerRegistry.BUCKET_VOLUME, false);
            if (drained != null && drained.amount > 0) {
                ItemStack filledContainer = FluidContainerRegistry.fillFluidContainer(drained, heldItem);
                if (filledContainer != null) {
                    tank.drain(face, drained.amount, true);
                    if (!player.capabilities.isCreativeMode) {
                        deductHeldAndGiveItem(player, heldItem, filledContainer);
                    }
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    /**
     * Consumes one item from the player's held stack (using the ORIGINAL heldItem snapshot,
     * NOT re-reading the inventory — both client and server must agree on the same original)
     * and puts the {@code result} in its place or in the inventory.
     */
    public static void deductHeldAndGiveItem(EntityPlayer player, ItemStack originalHeld, ItemStack result) {
        if (!player.capabilities.isCreativeMode) {
            if (originalHeld.stackSize <= 1) {
                // Original had 1 item: fully consumed → replace the slot with the result
                player.inventory.setInventorySlotContents(player.inventory.currentItem, result);
            } else {
                // Original had > 1: keep the same type in hand with reduced stack
                ItemStack remaining = originalHeld.copy();
                remaining.stackSize = originalHeld.stackSize - 1;
                player.inventory.setInventorySlotContents(player.inventory.currentItem, remaining);
                if (!player.inventory.addItemStackToInventory(result)) {
                    EntityItem itemEntity = player.entityDropItem(result, 0.0F);
                    if (itemEntity != null) {
                        itemEntity.delayBeforeCanPickup = 0;
                    }
                }
            }
        }
    }
}
