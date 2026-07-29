# Phase 14 -- Quantity label (quantify key)

## Status: **COMPLETED** (build verified; manual in-game test pending)

## Source File(s) (modified)
- src/main/java/com/mrfuzzihead/fluiddrawers/block/BlockTank.java (added `ModItems.quantifyKey` branch to the held-item dispatcher)
- src/main/java/com/mrfuzzihead/fluiddrawers/client/tesr/RenderTileTank.java (added `renderQuantityLabel` + `renderLabelText`; replaced the Phase 7 TODO with the call)
- src/main/java/com/mrfuzzihead/fluiddrawers/item/block/ItemBlockTank.java (added "Quantified" tooltip indicator)
- src/main/java/com/mrfuzzihead/fluiddrawers/integration/WailaIntegration.java (added "Quantified" WAILA indicator)
- src/main/resources/assets/fluiddrawers/lang/en_US.lang (added `fluiddrawers.tooltip.quantified`)

## Pre-existing infrastructure (no changes needed)
- src/main/java/com/mrfuzzihead/fluiddrawers/util/SimpleDrawerAttributes.java (`showingQty` field, `isShowingQuantity()`/`setShowingQuantity()`, NBT key `"quant"` -- from Phase 4)
- src/main/java/com/mrfuzzihead/fluiddrawers/Config.java (`quantifyShowsFluidName` -- from Phase 4)
- src/main/java/com/mrfuzzihead/fluiddrawers/tile/TileTank.java (vanilla `getDescriptionPacket`/`onDataPacket` + `writeToPortableNBT`/`readFromPortableNBT` carry the `"Attributes"` tag incl. `"quant"` AND the `"Drawer"` fluid NBT -- from Phases 6/12)

## Dependent classes (what breaks if this changes)
- `BlockTank.onBlockActivated` dispatcher ordering -- the quantify branch is inserted after the shroud-key and before the personal-key branches (SD 1.7.10 `BlockDrawers` order: lock → shroud → quantify → personal). Reordering earlier checks would violate the section-4 security-first contract.
- `RenderTileTank.renderQuantityLabel` reads `isShowingQuantity()` + `isConcealed()` + the fluid amount client-side every frame; it depends on the attribute + fluid being synced (they are, via the vanilla description packet).

## API delta rulebook rows that apply
- SD 1.7.10 item remap: `ModItems.quantifyKey` (`ItemQuantifyKey`) is the "Quantify Key"; 1.12.2 FD has no quantify-key item (the attribute is toggled by SD's key). `IQuantifiable` exists in SD 1.7.10 API but our `SimpleDrawerAttributes` is a standalone concrete class (SD GTNH 2.2.26 lacks the unified `IDrawerAttributes` interface), so we toggle `setShowingQuantity` directly -- matches the 1.12.2 FD attribute model.
- **Networking delta (justified deviation):** the 1.12.2 custom `SimpleNetworkWrapper` packets (`SPacketSyncFluidDrawerCount` + `SPacketSyncFluidDrawerFluid`) are **NOT ported**. They existed because Chameleon's `ChamTileEntity` split TE data such that the vanilla description packet did not carry the fluid/count for the TESR. Our unified portable NBT (`TileTank.writeToPortableNBT` writes `"Drawer"` fluid NBT + `"Attributes"` incl. `"quant"`) is fully synced by the Phase 6 vanilla `getDescriptionPacket`/`onDataPacket` path, so the custom packets are redundant. Documented in the master-migration-spec Phase 14 DoD.
- 1.12.2 `GlStateManager` → 1.7.10 `GL11`; `FontRenderer.func_78256_a` → `getStringWidth`; `field_78288_b` → `FONT_HEIGHT`; `func_78276_b` → `drawString` (no-shadow variant). All verified against the decompiled 1.7.10 source (not guessed).
- Static-mesh re-render note: NOT applicable -- the label is a dynamic TESR (re-renders every frame from synced TE state), so `markBlockForUpdate` (which triggers the description packet) is sufficient.

## Acceptance Criteria
1. Right-click a tank (with fluid) holding the Quantify Key → a floating "Water / 1,000 mB" label appears on all four sides. PASS (build; pending manual visual check)
2. Right-click again → label disappears. PASS (build; pending manual visual check)
3. Label updates on fill/drain (reads the synced fluid amount each frame). PASS (build; pending manual check)
4. Label is hidden when the tank is concealed (concealment takes precedence). PASS (build; pending manual check)
5. `quantifyShowsFluidName=false` → only the "X mB" quantity line shows (no fluid name). PASS (build; pending manual check)
6. State persists across save/reload and sealed break/place (carried in the `"Attributes"` portable NBT). PASS (build; pending manual check)
7. "Quantified" indicator shows in the sealed-item tooltip + WAILA HUD. PASS (build; pending manual check)
8. Non-owner cannot toggle (security-first guard). PASS (build; pending manual check)
9. `./gradlew build` PASS (spotless + compile). PASS
