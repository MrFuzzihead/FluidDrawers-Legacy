package com.mrfuzzihead.fluiddrawers.mixins.late.storagedrawers;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityFramingTable;
import com.mrfuzzihead.fluiddrawers.block.BlockTankCustom;
import com.mrfuzzihead.fluiddrawers.item.block.ItemBlockTankCustom;

/**
 * Lets the framed Fluid Tank be placed in the StorageDrawers framing table's drawer slot. The SD
 * framing table only accepts {@code BlockDrawersCustom}/{@code BlockTrimCustom} via
 * {@code TileEntityFramingTable.isItemValidDrawer}; this mixin extends that check to
 * {@link BlockTankCustom} (mirrors FCD's {@code MixinTileEntityFramingTable}).
 *
 * <p>
 * The tank also re-purposes the front ("window") material slot: for a tank input, the front slot
 * only accepts glass blocks/panes (clear or stained), which tint the rendered window; side/trim
 * stay opaque-only. Glass is NOT made a generic material (so drawers are unaffected); shift-click
 * support for glass into the tank's front slot is handled by the container-side mixin
 * {@code MixinContainerFramingTable#fluiddrawers$shiftGlassToTankFront} instead.
 */
@Mixin(TileEntityFramingTable.class)
public class MixinTileEntityFramingTable {

    @Inject(
        method = "isItemValidDrawer(Lnet/minecraft/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private static void fluiddrawers$acceptFramedTank(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack == null || stack.getItem() == null) return;

        Block block = Block.getBlockFromItem(stack.getItem());
        if (block instanceof BlockTankCustom) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "isItemValidForSlot(ILnet/minecraft/item/ItemStack;)Z",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void fluiddrawers$tankSlotRules(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack == null || stack.getItem() == null) return;

        TileEntityFramingTable self = (TileEntityFramingTable) (Object) this;
        ItemStack input = self.getStackInSlot(0);
        if (input == null || input.getItem() == null) return;

        Block inputBlock = Block.getBlockFromItem(input.getItem());
        if (!(inputBlock instanceof BlockTankCustom)) return;

        if (slot == 0) {
            cir.setReturnValue(true);
        } else if (slot == 4) {
            cir.setReturnValue(false);
        } else if (slot == 3) {
            // Front = the tank window: glass (clear or stained) only.
            cir.setReturnValue(ItemBlockTankCustom.isGlassMaterial(stack));
        } else if (slot == 1 || slot == 2) {
            // Side / trim: opaque blocks only (glass goes in the front slot, not the frame).
            cir.setReturnValue(isOpaqueBlock(stack));
        } else {
            cir.setReturnValue(false);
        }
    }

    private static boolean isOpaqueBlock(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return false;
        Block block = Block.getBlockFromItem(stack.getItem());
        return block != null && block.isOpaqueCube();
    }
}
