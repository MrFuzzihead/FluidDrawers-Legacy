# Phase 15 -- Concealment key (shroud key)

## Status: **COMPLETED** (build verified; manual in-game test pending)

## Source File(s)
- src/main/java/com/mrfuzzihead/fluiddrawers/block/BlockTank.java (modified -- added `ModItems.shroudKey` branch to the held-item dispatcher)

## Pre-existing infrastructure (no changes needed)
- src/main/java/com/mrfuzzihead/fluiddrawers/util/SimpleDrawerAttributes.java (`concealed` field, `isConcealed()`/`setConcealed()`, NBT ser/des -- from Phase 4)
- src/main/java/com/mrfuzzihead/fluiddrawers/client/tesr/RenderTileTank.java (`renderFluid` early-returns on `isConcealed()` -- from Phase 7)
- src/main/java/com/mrfuzzihead/fluiddrawers/tile/TileTank.java (`getDescriptionPacket`/`onDataPacket` + `writeToPortableNBT`/`readFromPortableNBT` carry the `"Attributes"` tag including `concealed` -- from Phases 6/12)

## Dependent classes (what breaks if this changes)
- `BlockTank.onBlockActivated` dispatcher ordering -- the shroud branch is inserted between the lock-key and personal-key branches (SD 1.7.10 `BlockDrawers` order: lock → shroud → quantify → personal). Reordering earlier checks would violate the section-4 security-first contract.
- `RenderTileTank` reads `isConcealed()` client-side every frame; it depends on the attribute being synced (it is, via the description packet).

## API delta rulebook rows that apply
- SD 1.7.10 item remap: `ModItems.shroudKey` (`ItemShroudKey`) is the "Concealment Key"; 1.12.2 FD has no shroud-key item (concealment is an attribute toggled by SD's key). `IShroudable` exists in SD 1.7.10 API but our `SimpleDrawerAttributes` is a standalone concrete class (SD GTNH 2.2.26 lacks the unified interface), so we toggle `setConcealed` directly -- matches the 1.12.2 FD attribute model.
- `@Mod.EventBusSubscriber` → explicit register; `ItemStack.EMPTY` → null; `cpw.mods.fml.*` namespace -- all already handled by prior phases.
- Static-mesh re-render note: NOT applicable here -- the fluid is a dynamic TESR (re-renders every frame from synced TE state), so `markBlockForUpdate` (which triggers the description packet) is sufficient; no chunk-mesh refresh needed (unlike the Phase 10 vending texture swap / Phase 11 overlays which live in the static `BlockTankRenderer`).

## Acceptance Criteria
1. Right-click a tank (with fluid) holding the Concealment Key → fluid becomes invisible in-world (TESR skips `renderFluid`). PASS (build; pending manual visual check)
2. Right-click again → fluid re-appears at the correct level. PASS (build; pending manual visual check)
3. Toggling is immediate (no chunk reload needed) -- `markBlockForUpdate` pushes the attribute to the client. PASS (build; pending manual check)
4. State persists across save/reload and across sealed break/place (carried in the `"Attributes"` portable NBT). PASS (build; pending manual check)
5. Non-owner cannot toggle (security-first guard). PASS (build; pending manual check)
6. `./gradlew build` PASS (spotless + compile). PASS
