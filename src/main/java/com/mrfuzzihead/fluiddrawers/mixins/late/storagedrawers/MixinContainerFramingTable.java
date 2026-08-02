package com.mrfuzzihead.fluiddrawers.mixins.late.storagedrawers;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jaquadro.minecraft.storagedrawers.inventory.ContainerFramingTable;
import com.mrfuzzihead.fluiddrawers.block.BlockTankCustom;
import com.mrfuzzihead.fluiddrawers.item.block.ItemBlockTankCustom;

/**
 * Produces a framed Fluid Tank in the StorageDrawers framing table output when the input slot holds
 * a framed tank and side material is present. Mirrors FCD's {@code MixinContainerFramingTable}, but
 * builds the result via {@link ItemBlockTankCustom#makeFramedTankStack} (the tank's item is not an
 * {@code ItemCustomDrawers}, so {@code ItemCustomDrawers.makeItemStack} would return null).
 *
 * <p>
 * Also handles shift-clicking glass into the tank's front ("window") slot. Because the glass-front
 * ability is tank-exclusive, {@code TileEntityFramingTable.isItemValidMaterial} is NOT extended
 * (glass is not a generic material -- drawers are unaffected); instead this mixin gives
 * {@code transferStackInSlot} a tank-scoped path that merges glass only into the front slot.
 *
 * <p>
 * Shadowing note: only fields declared directly in {@link ContainerFramingTable} are shadowed
 * (matching the repo's other SD mixins). Inherited {@code Container} members (e.g.
 * {@code mergeItemStack}) do not resolve at apply time ("was not located in the target class"), so
 * the front-slot merge is done manually against the public {@link Slot} / {@link ItemStack} API.
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

    @Shadow(remap = false)
    private List<Slot> playerSlots;

    @Shadow(remap = false)
    private List<Slot> hotbarSlots;

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

    @Inject(
        method = "transferStackInSlot(Lnet/minecraft/entity/player/EntityPlayer;I)Lnet/minecraft/item/ItemStack;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false)
    private void fluiddrawers$shiftGlassToTankFront(EntityPlayer player, int slotIndex,
        CallbackInfoReturnable<ItemStack> cir) {
        if (playerSlots.isEmpty() || hotbarSlots.isEmpty()) return;
        int inventoryStart = playerSlots.get(0).slotNumber;
        int hotbarStart = hotbarSlots.get(0).slotNumber;
        if (slotIndex < inventoryStart) return;

        // Locate the source slot within the player inventory / hotbar ranges (no shadowing of the
        // inherited Container.inventorySlots field, which fails to resolve in the mixin).
        Slot slot;
        if (slotIndex < hotbarStart) {
            int idx = slotIndex - inventoryStart;
            if (idx < 0 || idx >= playerSlots.size()) return;
            slot = playerSlots.get(idx);
        } else {
            int idx = slotIndex - hotbarStart;
            if (idx < 0 || idx >= hotbarSlots.size()) return;
            slot = hotbarSlots.get(idx);
        }
        if (slot == null || !slot.getHasStack()) return;
        ItemStack slotStack = slot.getStack();
        if (!ItemBlockTankCustom.isGlassMaterial(slotStack)) return;

        ItemStack input = tableInventory.getStackInSlot(inputSlot.getSlotIndex());
        if (input == null || input.getItem() == null) return;
        Block block = Block.getBlockFromItem(input.getItem());
        if (!(block instanceof BlockTankCustom)) return;

        // Merge the glass into the tank's front (window) slot only, mirroring the empty-slot and
        // stack-size merge semantics of Container.mergeItemStack but without shadowing inherited
        // Container members (which fail to resolve in the mixin). The front slot's validation
        // (Slot.isItemValid -> tile isItemValidForSlot) is honoured in the empty-slot branch.
        ItemStack itemStack = slotStack.copy();
        Slot frontSlot = materialFrontSlot;
        ItemStack frontStack = frontSlot.getStack();

        if (frontStack == null) {
            if (!frontSlot.isItemValid(slotStack)) return;
            int max = Math.min(slotStack.stackSize, frontSlot.getSlotStackLimit());
            ItemStack placed = slotStack.splitStack(max);
            frontSlot.putStack(placed);
            frontSlot.onSlotChanged();
            if (slotStack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
            slot.onPickupFromSlot(player, slotStack);
            cir.setReturnValue(itemStack);
        } else if (frontStack.isItemEqual(slotStack) && ItemStack.areItemStackTagsEqual(frontStack, slotStack)) {
            int move = Math.min(slotStack.stackSize, frontStack.getMaxStackSize() - frontStack.stackSize);
            if (move <= 0) return;
            frontStack.stackSize += move;
            slotStack.stackSize -= move;
            frontSlot.onSlotChanged();
            if (slotStack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }
            slot.onPickupFromSlot(player, slotStack);
            cir.setReturnValue(itemStack);
        }
        // else: front slot holds a different glass type -> fall through to the base implementation,
        // which shuffles the stack normally within the player inventory.
    }
}
