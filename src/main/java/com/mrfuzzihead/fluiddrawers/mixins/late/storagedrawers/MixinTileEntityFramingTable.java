package com.mrfuzzihead.fluiddrawers.mixins.late.storagedrawers;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityFramingTable;
import com.mrfuzzihead.fluiddrawers.block.BlockTankCustom;

/**
 * Lets the framed Fluid Tank be placed in the StorageDrawers framing table's drawer slot. The SD
 * framing table only accepts {@code BlockDrawersCustom}/{@code BlockTrimCustom} via
 * {@code TileEntityFramingTable.isItemValidDrawer}; this mixin extends that check to
 * {@link BlockTankCustom} (mirrors FCD's {@code MixinTileEntityFramingTable}).
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
}
