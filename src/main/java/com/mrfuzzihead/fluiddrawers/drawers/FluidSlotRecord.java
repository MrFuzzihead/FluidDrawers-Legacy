package com.mrfuzzihead.fluiddrawers.drawers;

import com.jaquadro.minecraft.storagedrawers.api.storage.attribute.LockAttribute;
import com.mrfuzzihead.fluiddrawers.util.SimpleDrawerAttributes;

/**
 * A single fluid drawer on a controller's network, plus the metadata needed to order it for
 * insertion/extraction. Ports the 1.12.2 {@code FluidDrawerController.FluidSlotRecord} priority
 * logic 1:1 so that connected Fluid Tanks are filled/drained in the same priority order the
 * 1.12.2 controller used (populated-locked first, void last, etc.).
 *
 * <p>
 * Held as a top-level class (rather than a mixin nested class) to avoid any mixin
 * nested-class emission pitfalls — the
 * {@link com.mrfuzzihead.fluiddrawers.mixins.late.storagedrawers.MixinTileEntityController}
 * constructs these during its fluid-cache rebuild.
 * </p>
 */
public class FluidSlotRecord implements Comparable<FluidSlotRecord> {

    // Priority constants matching the 1.12.2 FluidDrawerController.FluidSlotRecord. Lower value
    // = filled first / drained last. Sorted ascending so populated-locked tanks win insertion.
    static final int PRI_LOCKED = 0;
    static final int PRI_LOCKED_VOID = 1;
    static final int PRI_NORMAL = 2;
    static final int PRI_VOID = 3;
    static final int PRI_EMPTY = 4;
    static final int PRI_LOCKED_EMPTY = 5;

    public final FluidDrawer drawer;
    private final SimpleDrawerAttributes attrs;
    private final int x;
    private final int y;
    private final int z;
    public final int priority;

    public FluidSlotRecord(FluidDrawer drawer, SimpleDrawerAttributes attrs, int x, int y, int z) {
        this.drawer = drawer;
        this.attrs = attrs;
        this.x = x;
        this.y = y;
        this.z = z;
        this.priority = computePriority();
    }

    private int computePriority() {
        if (drawer.isEmpty()) {
            return attrs.isItemLocked(LockAttribute.LOCK_EMPTY) ? PRI_LOCKED_EMPTY : PRI_EMPTY;
        }
        if (attrs.isVoid()) {
            return attrs.isItemLocked(LockAttribute.LOCK_POPULATED) ? PRI_LOCKED_VOID : PRI_VOID;
        }
        return attrs.isItemLocked(LockAttribute.LOCK_POPULATED) ? PRI_LOCKED : PRI_NORMAL;
    }

    @Override
    public int compareTo(FluidSlotRecord other) {
        int diff = this.priority - other.priority;
        if (diff != 0) return diff;
        // Deterministic tiebreak by position (matches the 1.12.2 BlockPos.compareTo intent).
        if (this.x != other.x) return Integer.compare(this.x, other.x);
        if (this.y != other.y) return Integer.compare(this.y, other.y);
        return Integer.compare(this.z, other.z);
    }
}
