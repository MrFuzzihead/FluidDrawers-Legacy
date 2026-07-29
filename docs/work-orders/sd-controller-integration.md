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

