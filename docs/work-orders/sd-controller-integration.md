# Work Order — SD Controller Integration (stretch milestone)

## Source file(s)
- `src/main/java/com/mrfuzzihead/fluiddrawers/mixins/late/storagedrawers/MixinTileEntityController.java` (NEW)
- `src/main/java/com/mrfuzzihead/fluiddrawers/drawers/FluidSlotRecord.java` (NEW — top-level port of 1.12.2 `FluidSlotRecord`)
- `src/main/java/com/mrfuzzihead/fluiddrawers/mixins/Mixins.java` (EDIT — register the mixin)
- `src/main/java/com/mrfuzzihead/fluiddrawers/block/BlockTank.java` (EDIT — `implements INetworked`)
- `src/main/java/com/mrfuzzihead/fluiddrawers/drawers/DrawerReflect.java` (DELETED — reflection obsolete)

## Dependent classes / what breaks if this changes
- Anything that right-clicks a StorageDrawers `TileEntityController` or its `BlockController`
  (none in our mod; the mixin only *adds* behavior, never removes the existing item-network logic).
- Anything that pipes fluid into/out of the controller block (now an `IFluidHandler`).
- `TileTank` / `BlockTank`: the mixin discovers tanks via `te instanceof FluidDrawerHost` and
  `block instanceof INetworked`, so `BlockTank` MUST stay `INetworked` and `TileTank` MUST stay a
  `FluidDrawerHost` exposing a valid `FluidDrawerGroup` (no changes were needed to `TileTank`).

## API delta rulebook rows that apply
- **Forge capabilities → 1.7.10 IFluidHandler**: no `AttachCapabilitiesEvent`/`ICapabilityProvider`;
  the controller mixin implements `IFluidHandler(fill/drain/canFill/canDrain/getTankInfo)` directly.
- **FluidContainerRegistry** (1.7.10 fluid containers): right-click uses
  `BlockInteractionUtils.transferFluid` (already built in Phase 5), same as the tank itself.
- **1.12.2 coremod ASM → Mixins**: the `TileEntityController` ASM target is now a UniMixins mixin.
- **Avoid reflection**: the 1.12.2 `DrawerReflect` (libnine `MirrorUtils` into the private `storage`
  map) is NOT ported. `BlockCoord` is package-private and `StorageRecord` is a private inner class,
  so they are inaccessible to a mixin source in our package — the mixin runs its own INetworked BFS
  instead (only the primitive `range` field is `@Shadow(remap=false)`-ed).

## Acceptance criteria
- [x] `./gradlew spotlessApply build` PASS, zero mixin-AP warnings, empty refmap (correct for
      mod-class `@Inject`/`@Shadow` targets with `remap=false`).
- [x] `BlockTank implements INetworked` (controller network geometry routes through tanks).
- [x] Controller exposes `IFluidHandler` (mixins add the interface + 6 methods).
- [x] Right-click controller front face with a registered fluid container → fills/drains connected
      tanks (via `interactPutItemsIntoInventory` HEAD inject); non-container items & key items behave
      exactly as before (inject returns early / doesn't cancel).
- [x] Fluid routing is priority-sorted and respects lock/void (`bypass=false`), matching 1.12.2.
- [x] No reflection anywhere in the integration path.
- [ ] **Pending `runClient`**: in-game verify (1) place controller + adjacent fluid tank, right-click
      controller front with water bucket → tank fills; (2) empty bucket on controller → tank drains;
      (3) pipe pumping into controller block → distributed to connected tanks.

## Bug found during in-game test (2026-07-28) — FIXED
Right-clicking the controller face with a water bucket did nothing. Root cause: `TargetMods.STORAGEDRAWERS`
was registered with mod id `"storagedrawers"` (lowercase), but StorageDrawers declares
`@Mod(modid = "StorageDrawers")` (mixed case). `TargetModBuilder.isTargetPresent()` does a
**case-sensitive** `loadedMods.contains(modId)`, so the required-mod check failed and the
`STORAGEDRAWERS` late-mixin entry was silently filtered out (logged under "Not loading the following
LATE mixins"). Result: `MixinTileEntityController` was never applied → the controller had no
`IFluidHandler` and the `interactPutItemsIntoInventory` inject didn't exist → the bucket did nothing
(no crash, since the un-mixed original method just returned 0 for a fluid-tank-only network).
**Fix:** `TargetMods.STORAGEDRAWERS("StorageDrawers")`. Build still PASS. **Pending re-test.**

## Second bug exposed once the mixin applied (2026-07-28) — FIXED
After the mod-id fix, the game crashed at startup with `InvalidInjectionException: @Inject
::fluiddrawers$markFluidCacheDirty()V — Expected (CallbackInfo;)V but found ()V`. Cause: a
`@Inject` handler into a `void`-returning target (`updateCache()V`) MUST take a `CallbackInfo`
parameter, but the handler had none. (The AP didn't catch it because `remap=false` relaxes its
descriptor validation; the runtime transformer enforces it.) This crash took down the whole
StorageDrawers mod (`NoClassDefFoundError: TileEntityController`) because the failed mixin apply
happens during SD's class load. **Fix:** added `(CallbackInfo ci)` to `fluiddrawers$markFluidCacheDirty`
+ the `CallbackInfo` import. The other inject (`interceptFluidContainer` into the `int`-returning
`interactPutItemsIntoInventory`) already correctly takes `CallbackInfoReturnable<Integer>`. Build PASS.

## Third finding — drain-to-zero visual desync (2026-07-28) — FIXED
Draining the LAST bucket via the controller "technically" drained (bucket filled, server state 0)
but the tank render still showed 1 bucket. Cause: an **asymmetry in the fluid NBT sync**.
`SimpleFluidDrawer.serializeNBT` only writes the `"Fluid"` tag when `fluid != null`; draining an
unlocked tank to zero sets `fluid = null`, so the tag is **omitted**. `SimpleFluidDrawer.deserializeNBT`
only READ the fluid when the tag was present — it never CLEARED it when absent, so an already-loaded
client drawer retained the stale fluid. Fill worked (tag present); drain-to-zero didn't (tag absent).
Direct bucket-on-tank avoided this only because `BlockTank.onBlockActivated` runs `transferFluid` on
the client too (client-side prediction mutates the drawer directly), whereas the controller path runs
`interactPutItemsIntoInventory` server-only (`!world.isRemote`) and relies on the description packet.
`Block.hasTileEntity` already returns true (Forge: `isTileProvider`, since BlockTank implements
ITileEntityProvider), so the packet WAS sent — the bug was purely the client not clearing on
absent-tag. **Fix:** `deserializeNBT` now sets `this.fluid = null` in the `else` branch. This also
fixes the latent drain-to-zero + chunk-reload case. Build PASS.

## Fourth finding — filled container invisible in inventory (stack-of-2+) (2026-07-28) — FIXED
Right-clicking the controller front face with a STACK of 2+ empty buckets filled one bucket
(server state correct) but the resulting filled bucket was invisible in the inventory until the
player clicked around. Cause: `BlockInteractionUtils.deductHeldAndGiveItem`, for `stackSize > 1`,
decrements the held slot then calls `addItemStackToInventory(filledContainer)` — placing the filled
bucket in a FREE inventory slot. The vanilla post-block-use sync only refreshes the HELD slot (which
is why it worked for the single-bucket case where the filled bucket replaces the held slot, and why
the held -1-empty-bucket slot always showed). The free-slot add was never synced. Direct
bucket-on-tank hid this via client-side prediction (BlockTank runs transferFluid on both sides). **Fix:**
the controller inject now captures `transferFluid`'s boolean return and, on success, calls
`((EntityPlayerMP) player).sendContainerToPlayer(player.inventoryContainer)` — mirroring SD's own
`CommonProxy.updatePlayerInventory` (used by TileEntityController's dump path and TileEntityDrawers),
which forces a full `S30PacketWindowItems` inventory re-send. Build PASS.

