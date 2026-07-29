package com.mrfuzzihead.fluiddrawers.mixins.late.storagedrawers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.jaquadro.minecraft.storagedrawers.api.storage.INetworked;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityController;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerGroup;
import com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerHost;
import com.mrfuzzihead.fluiddrawers.drawers.FluidSlotRecord;
import com.mrfuzzihead.fluiddrawers.util.BlockInteractionUtils;
import com.mrfuzzihead.fluiddrawers.util.SimpleDrawerAttributes;

/**
 * Integrates Fluid Tanks with the StorageDrawers controller. Does for 1.7.10 what the 1.12.2
 * {@code FluidDrawerController} + {@code ControllerFluidCapabilityHandler} + {@code DrawerReflect}
 * trio did via the Forge capability system and reflection — but without capabilities (none in
 * 1.7.10) and without reflection.
 *
 * <h3>How it replaces reflection</h3>
 * The 1.12.2 version reflected into the controller's private {@code storage} map
 * ({@code Map<BlockCoord, StorageRecord>}) to discover which connected groups were fluid groups.
 * In 1.7.10 those types ({@code BlockCoord} is package-private, {@code StorageRecord} is a private
 * inner class) are inaccessible to a mixin source in our package, so reflecting/shadowing them is
 * off the table. Instead, this mixin runs its own network BFS (the same INetworked traversal
 * {@code TileEntityController.populateNodes} uses) to collect connected {@link FluidDrawerHost}
 * tanks directly. No private fields of inaccessible types are touched — only the primitive
 * {@code range} field is shadowed. This is the "avoid reflection" solution.
 *
 * <h3>What it adds</h3>
 * <ul>
 * <li>Implements {@link IFluidHandler} on the controller so fluid routing (fill/drain across all
 * connected tanks, priority-sorted) and automation (pipes/hoppers pumping into the
 * controller block) both work. Routing uses the {@link FluidDrawer} API with
 * {@code bypass=false} so lock/void rules are respected, matching the 1.12.2 controller.</li>
 * <li>Intercepts {@code interactPutItemsIntoInventory} so right-clicking the controller's front
 * face with a fluid container (bucket, registered fluid holder, ...) fills/drains the
 * connected tanks instead of attempting item insertion — the 1.12.2
 * {@code ControllerFluidCapabilityHandler} behaviour.</li>
 * </ul>
 *
 * <p>
 * Fluid Tanks must be {@link INetworked} blocks (see {@code BlockTank}) so the network geometry
 * routes through them; the BFS here then discovers them as {@code FluidDrawerHost}s.
 * </p>
 */
@Mixin(TileEntityController.class)
public abstract class MixinTileEntityController extends TileEntity implements IFluidHandler {

    // The controller's search range (set from StorageDrawers config in its constructor). Shadowed
    // directly — no reflection — to bound the fluid-tank BFS the same way populateNodes is bounded.
    // remap=false: this is a StorageDrawers (mod) field, not a vanilla member, so no SRG refmap
    // lookup applies — the name is used as-is at runtime.
    @Shadow(remap = false)
    private int range;

    // worldObj / xCoord / yCoord / zCoord are inherited from TileEntity (the target's own
    // superclass), so declaring this mixin `extends TileEntity` makes them directly visible at
    // compile time AND lets the reobf step map them to SRG field names correctly — no @Shadow /
    // refmap needed (which avoids the "Cannot find target for @Shadow field" production hazard).

    /** Priority-sorted snapshot of every fluid drawer on the network, rebuilt when the cache is dirty. */
    private List<FluidSlotRecord> fluiddrawers$fluidSlots;

    /** True when the network may have changed and fluidSlots needs rebuilding. */
    private boolean fluiddrawers$fluidCacheDirty;

    // ------------------------------------------------------------------
    // Cache lifecycle
    // ------------------------------------------------------------------

    /**
     * The controller rebuilds its item-storage cache on network changes; mirror that here so the
     * fluid cache stays in sync. Lazy: fluidSlots is null until first use (mixin field defaults).
     */
    @Inject(method = "updateCache", at = @At("RETURN"), remap = false)
    private void fluiddrawers$markFluidCacheDirty(CallbackInfo ci) {
        this.fluiddrawers$fluidCacheDirty = true;
    }

    private void fluiddrawers$ensureFluidCache() {
        if (this.fluiddrawers$fluidSlots == null) {
            this.fluiddrawers$fluidSlots = new ArrayList<>();
            this.fluiddrawers$fluidCacheDirty = true;
        }
        if (this.fluiddrawers$fluidCacheDirty) {
            fluiddrawers$rebuildFluidCache();
            this.fluiddrawers$fluidCacheDirty = false;
        }
    }

    /**
     * BFS over INetworked blocks within {@link #range}, collecting every connected
     * {@link FluidDrawerHost}'s fluid drawers into {@link #fluiddrawers$fluidSlots}. This is the
     * same traversal {@code TileEntityController.populateNodes} uses for item storage, replicated
     * here because the controller's private {@code storage}/{@code StorageRecord} types are
     * inaccessible to this mixin source (hence the reflection-free re-scan).
     */
    private void fluiddrawers$rebuildFluidCache() {
        List<FluidSlotRecord> slots = this.fluiddrawers$fluidSlots;
        slots.clear();

        if (worldObj == null) return;

        Queue<int[]> queue = new ArrayDeque<>();
        Set<Long> discovered = new HashSet<>();
        queue.add(new int[] { xCoord, yCoord, zCoord });
        discovered.add(fluiddrawers$coordKey(xCoord, yCoord, zCoord));

        int[][] neighbours = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };

        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            int depth = Math.max(Math.max(Math.abs(c[0] - xCoord), Math.abs(c[1] - yCoord)), Math.abs(c[2] - zCoord));
            if (depth > range) continue;

            Block block = worldObj.getBlock(c[0], c[1], c[2]);
            if (!(block instanceof INetworked)) continue;

            TileEntity te = worldObj.getTileEntity(c[0], c[1], c[2]);
            if (te instanceof FluidDrawerHost) {
                FluidDrawerHost host = (FluidDrawerHost) te;
                FluidDrawerGroup group = host.getFluidDrawerGroup();
                if (group != null && group.isFluidDrawerGroupValid()) {
                    SimpleDrawerAttributes attrs = host.getAttributes();
                    for (int slot = 0, n = group.getFluidDrawerCount(); slot < n; slot++) {
                        FluidDrawer drawer = group.getFluidDrawer(slot);
                        if (drawer != null) {
                            slots.add(new FluidSlotRecord(drawer, attrs, c[0], c[1], c[2]));
                        }
                    }
                }
            }

            for (int[] o : neighbours) {
                int nx = c[0] + o[0];
                int ny = c[1] + o[1];
                int nz = c[2] + o[2];
                long key = fluiddrawers$coordKey(nx, ny, nz);
                if (!discovered.contains(key)) {
                    discovered.add(key);
                    queue.add(new int[] { nx, ny, nz });
                }
            }
        }

        Collections.sort(slots);
    }

    /**
     * Packs a coordinate into a long for the BFS visited-set. Encodes the delta from the controller
     * position, masked to 20 bits per axis — collision-free for any sane controller range (the
     * controller never scans more than {@code range} blocks, and {@code range} is a small config
     * value, well under 2^20).
     */
    private long fluiddrawers$coordKey(int x, int y, int z) {
        return (((long) (x - xCoord)) & 0xFFFFFL) << 40 | (((long) (y - yCoord)) & 0xFFFFFL) << 20
            | (((long) (z - zCoord)) & 0xFFFFFL);
    }

    // ------------------------------------------------------------------
    // Fluid-container right-click interception
    // ------------------------------------------------------------------

    /**
     * If the player is holding a registered fluid container (filled or empty), route it through the
     * controller's own {@link IFluidHandler} so it fills/drains the connected tanks — and cancel so
     * the controller does not try to insert the container as an item. Mirrors the 1.12.2
     * {@code ControllerFluidCapabilityHandler.handleTankInteraction} (which cancelled unconditionally
     * for any item exposing the fluid-handler-item capability).
     *
     * <p>
     * This runs only on the server: {@code BlockController.onBlockActivated} calls
     * {@code interactPutItemsIntoInventory} solely when {@code !world.isRemote}, after the key-item
     * and front-face checks — so fluid interaction is naturally gated to the controller's front face
     * and never pre-empts shroud/lock/personal/quantify keys.
     * </p>
     */
    @Inject(method = "interactPutItemsIntoInventory", at = @At("HEAD"), cancellable = true, remap = false)
    private void fluiddrawers$interceptFluidContainer(EntityPlayer player, CallbackInfoReturnable<Integer> cir) {
        ItemStack held = player.inventory.getCurrentItem();
        if (held == null) return;

        boolean isFluidContainer = FluidContainerRegistry.getFluidForFilledItem(held) != null
            || FluidContainerRegistry.isEmptyContainer(held);
        if (!isFluidContainer) return;

        boolean transferred = false;
        if (!worldObj.isRemote) {
            // bypass=false: the controller's IFluidHandler respects lock/void rules, matching the
            // 1.12.2 controller (which had no bypass path). ForgeDirection.UNKNOWN because the
            // controller routes to all connected tanks regardless of the clicked side.
            transferred = BlockInteractionUtils
                .transferFluid((IFluidHandler) this, player, held, ForgeDirection.UNKNOWN, false);
            // This inject runs server-side only (BlockController calls interactPutItemsIntoInventory
            // solely when !world.isRemote), so the client never predicts the inventory change —
            // unlike direct bucket-on-tank (BlockTank runs transferFluid on both sides). The vanilla
            // post-block-use sync only refreshes the HELD slot; a filled container placed into a free
            // inventory slot (stack-of-2+ empty buckets) would otherwise stay invisible until the
            // player clicks around. Mirror SD's CommonProxy.updatePlayerInventory: full inventory
            // re-send only when a transfer actually happened.
            if (transferred && player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer);
            }
        }
        // Cancel for fluid containers when a transfer actually happened, so the bucket is
        // never stored as an item in the controller's drawers. If the transfer failed (e.g.
        // tanks full when filling, or tanks empty when draining), fall through to SD's fallback
        // item-insertion — a player might want to store an empty or full bucket as an item in
        // a normal drawer connected to the controller. This is friendlier UX than the 1.12.2
        // FD approach (which always cancels), and the fluid-routing priority is retained:
        // fluid tanks get first dibs, item drawers are the fallback.
        if (transferred) {
            cir.setReturnValue(0);
        }
    }

    // ------------------------------------------------------------------
    // IFluidHandler — route across all connected Fluid Tanks
    // ------------------------------------------------------------------

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (resource == null || resource.amount <= 0) return 0;
        fluiddrawers$ensureFluidCache();
        FluidStack sim = resource.copy();
        int filled = 0;
        for (FluidSlotRecord record : fluiddrawers$fluidSlots) {
            if (sim.amount <= 0) break;
            int transferred = record.drawer.insertFluid(sim, doFill, false);
            if (transferred > 0) {
                sim.amount -= transferred;
                filled += transferred;
            }
        }
        return filled;
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        if (resource == null || resource.amount <= 0) return null;
        fluiddrawers$ensureFluidCache();
        Fluid fluid = resource.getFluid();
        FluidStack sim = resource.copy();
        int drained = 0;
        for (FluidSlotRecord record : fluiddrawers$fluidSlots) {
            if (sim.amount <= 0) break;
            FluidStack extracted = record.drawer.extractFluid(sim, doDrain, false);
            if (extracted != null && extracted.amount > 0) {
                sim.amount -= extracted.amount;
                drained += extracted.amount;
            }
        }
        if (drained <= 0) return null;
        return new FluidStack(fluid, drained);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        if (maxDrain <= 0) return null;
        fluiddrawers$ensureFluidCache();
        // Find the first drawer holding any fluid, then drain that fluid type up to maxDrain across
        // all matching drawers (mirrors the 1.12.2 FluidDrawerController.drain(int, boolean)).
        for (FluidSlotRecord record : fluiddrawers$fluidSlots) {
            FluidStack stored = record.drawer.getStoredFluid();
            if (stored != null && stored.amount > 0) {
                FluidStack request = stored.copy();
                request.amount = maxDrain;
                return drain(from, request, doDrain);
            }
        }
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        if (fluid == null) return false;
        fluiddrawers$ensureFluidCache();
        for (FluidSlotRecord record : fluiddrawers$fluidSlots) {
            FluidStack stored = record.drawer.getStoredFluid();
            if (stored == null) return true;
            if (stored.getFluid() == fluid && record.drawer.canFluidBeStored(stored)) return true;
        }
        return false;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        if (fluid == null) return false;
        fluiddrawers$ensureFluidCache();
        for (FluidSlotRecord record : fluiddrawers$fluidSlots) {
            FluidStack stored = record.drawer.getStoredFluid();
            if (stored != null && stored.getFluid() == fluid && stored.amount > 0) return true;
        }
        return false;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        fluiddrawers$ensureFluidCache();
        FluidTankInfo[] info = new FluidTankInfo[fluiddrawers$fluidSlots.size()];
        for (int i = 0; i < info.length; i++) {
            FluidDrawer drawer = fluiddrawers$fluidSlots.get(i).drawer;
            FluidStack stored = drawer.getStoredFluid();
            info[i] = new FluidTankInfo(stored != null ? stored.copy() : null, drawer.getAcceptingMaxCapacity(stored));
        }
        return info;
    }
}
