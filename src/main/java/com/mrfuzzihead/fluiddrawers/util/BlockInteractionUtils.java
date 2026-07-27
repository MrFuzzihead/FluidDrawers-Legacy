package com.mrfuzzihead.fluiddrawers.util;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;

public class BlockInteractionUtils {

    public static boolean transferFluid(IFluidHandler tank, EntityPlayer player, ForgeDirection face, boolean bypass) {
        if (bypass && tank instanceof BypassableFluidHandler) {
            tank = new BypassingFluidHandlerWrapper((BypassableFluidHandler) tank);
        }

        ItemStack heldItem = player.inventory.getCurrentItem();
        if (heldItem == null) {
            return false;
        }

        // Try to empty a filled container into the tank
        FluidStack containedFluid = FluidContainerRegistry.getFluidForFilledItem(heldItem);
        if (containedFluid != null) {
            int filled = tank.fill(face, containedFluid, true);
            if (filled > 0) {
                if (!player.capabilities.isCreativeMode) {
                    ItemStack emptyContainer = FluidContainerRegistry.drainFluidContainer(heldItem);
                    if (emptyContainer != null) {
                        deductHeldAndGiveItem(player, emptyContainer);
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
                        deductHeldAndGiveItem(player, filledContainer);
                    }
                    return true;
                }
            }
            return false;
        }

        return false;
    }

    public static void deductHeldAndGiveItem(EntityPlayer player, ItemStack result) {
        if (!player.capabilities.isCreativeMode) {
            ItemStack heldStack = player.inventory.getCurrentItem();
            if (heldStack != null) {
                heldStack.stackSize--;
                if (heldStack.stackSize <= 0) {
                    player.inventory.setInventorySlotContents(player.inventory.currentItem, result);
                } else if (!player.inventory.addItemStackToInventory(result)) {
                    EntityItem itemEntity = player.entityDropItem(result, 0.0F);
                    if (itemEntity != null) {
                        itemEntity.delayBeforeCanPickup = 0;
                    }
                }
            }
        }
    }
}
