package com.mrfuzzihead.fluiddrawers.mixins.late.storagedrawers;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.jaquadro.minecraft.storagedrawers.inventory.ContainerFramingTable;
import com.mrfuzzihead.fluiddrawers.block.BlockTankCustom;
import com.mrfuzzihead.fluiddrawers.item.block.ItemBlockTankCustom;

/**
 * Produces a framed Fluid Tank in the StorageDrawers framing table output when the input slot holds
 * a framed tank and side material is present. Mirrors FCD's {@code MixinContainerFramingTable}, but
 * builds the result via {@link ItemBlockTankCustom#makeFramedTankStack} (the tank's item is not an
 * {@code ItemCustomDrawers}, so {@code ItemCustomDrawers.makeItemStack} would return null).
 */
@Mixin(ContainerFramingTable.class)
public class MixinContainerFramingTable {

    @Shadow(remap = false)
    private IInventory tableInventory;

    @Shadow(remap = false)
    private IInventory craftResult;

    @Shadow(remap = false)
    private Slot inputSlot;

    @Shadow(remap = false)
    private Slot materialSideSlot;

    @Shadow(remap = false)
    private Slot materialTrimSlot;

    @Shadow(remap = false)
    private Slot materialFrontSlot;

    @Inject(
        method = "onCraftMatrixChanged(Lnet/minecraft/inventory/IInventory;)V",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void fluiddrawers$handleFramedTank(IInventory inventory, CallbackInfo ci) {
        ItemStack target = tableInventory.getStackInSlot(inputSlot.getSlotIndex());
        if (target == null) return;

        Block block = Block.getBlockFromItem(target.getItem());
        if (!(block instanceof BlockTankCustom)) return;

        ItemStack matSide = tableInventory.getStackInSlot(materialSideSlot.getSlotIndex());
        if (matSide == null) return;

        ItemStack matTrim = tableInventory.getStackInSlot(materialTrimSlot.getSlotIndex());
        ItemStack matFront = tableInventory.getStackInSlot(materialFrontSlot.getSlotIndex());

        ItemStack result = ItemBlockTankCustom.makeFramedTankStack(block, 1, matSide, matTrim, matFront);
        craftResult.setInventorySlotContents(0, result);
        ci.cancel();
    }
}
