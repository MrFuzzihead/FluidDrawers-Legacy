# Phase 12 -- Item-block NBT persistence + custom name

## Status: **IMPLEMENTED** (build verified, awaiting verify-phase)

## Source File(s)
- src/main/java/com/mrfuzzihead/fluiddrawers/item/block/ItemBlockTank.java (NEW)
- src/main/java/com/mrfuzzihead/fluiddrawers/block/BlockTank.java (modified)
- src/main/java/com/mrfuzzihead/fluiddrawers/tile/TileTank.java (modified)
- src/main/java/com/mrfuzzihead/fluiddrawers/init/ModBlocks.java (modified)
- src/main/java/com/mrfuzzihead/fluiddrawers/client/gui/GuiTank.java (modified)
- src/main/resources/assets/fluiddrawers/lang/en_US.lang (modified)

## Dependent classes
- `BlockTank` -- the block whose break/drop/harvest lifecycle changed.
- `TileTank` -- the tile whose portable NBT + custom name methods are new.
- `ModBlocks` -- registration now uses `ItemBlockTank.class`.
- `GuiTank` -- title now reads the tile's custom name.
- All Phase 5-11 callers of `TileTank.writeToNBT`/`readFromNBT` are unaffected (they now delegate to the portable methods).

## API delta rules applied
- **1.12.2 `ItemBlockTank` (libnine `L9ItemBlockStated`) → 1.7.10 `ItemBlock`**: plain `ItemBlock` + `placeBlockAt` override + `addInformation` tooltip.
- **1.12.2 `writeToPortableNBT`/`readFromPortableNBT` (Chameleon `ChamTileEntity`)**: reimplemented as plain methods on `TileTank` (no `super` call -- excludes id/coords so a placed item doesn't overwrite the new TE's position). SD 1.7.10 `BaseTileEntity` uses the same portable/fixed split.
- **1.12.2 `CustomNameData` (Chameleon shim) → inline `customName` field**: SD 1.7.10 has no `CustomNameData` class; inlined as a `String customName` field with NBT key `"CustomName"` (matching SD `TileEntityFramingTable`).
- **1.12.2 `IWorldNameable` → 1.7.10 inline**: custom name handled via `stack.setStackDisplayName` (item display tag) + `"CustomName"` in portable NBT; anvil rename detected via `stack.hasDisplayName()`.
- **1.12.2 `getDrops(NonNullList, IBlockAccess, BlockPos, IBlockState, int)` → 1.7.10 `getDrops(World, int, int, int, int, int)`**: returns `ArrayList<ItemStack>`.
- **1.7.10 harvest ordering**: `removedByPlayer(willHarvest)` defers block removal so `getDrops` can read the TE before `breakBlock` removes it (matching SD 1.7.10 `BlockDrawers`).
- **1.12.2 `ItemStack.EMPTY`/`isEmpty()` → 1.7.10 `null`/`== null`**: used throughout the drop/tooltip logic.
- **`dropBlockAsItem(World, int, int, int, ItemStack)`**: 1.7.10 `Block` method for spawning upgrade items on non-sealed break (matching SD `BlockDrawers.breakBlock`).

## Acceptance Criteria
1. **Sealed break preserves contents**: Break a sealed tank (with fluid + upgrades + lock applied) → dropped item carries `"Tile"` portable NBT; tooltip shows "Sealed" + fluid name/amount/effective-capacity + each upgrade + "Locked". PASS (build verified; tooltip logic implemented).
2. **Sealed place restores + unseals**: Place the sealed tank item → tile's fluid, upgrades, lock/key statuses, and custom name are restored; tank is no longer sealed (interactable). PASS (build verified; `placeBlockAt` + `readFromPortableNBT` + `setIsSealed(false)`).
3. **Non-sealed break destroys fluid + drops upgrades**: Break a non-sealed tank (with fluid + upgrades) → fluid is destroyed (no `"Tile"` NBT); upgrades are dropped on the ground (creative upgrades skipped); returned item is clean-slate. PASS (build verified; `getDrops` + `breakBlock` logic).
4. **Custom name (anvil)**: Anvil-rename a tank item → place it → tile carries the custom name; GUI title shows the custom name. PASS (build verified; `placeBlockAt` + `GuiTank` title).
5. **Custom name preserved on break**: Break a tank with a custom name → dropped item shows the custom name (display tag). PASS (build verified; `getDrops` `setStackDisplayName`).
6. **NBT survives reload**: Sealed tank item placed → save/reload → fluid + upgrades + lock still present. (Relies on Phase 6 sync + Phase 11 NBT; portable NBT uses the same `writeToNBT`/`readFromNBT` path.)

## Implementation Notes
- `TileTank.writeToNBT`/`readFromNBT` now delegate to `writeToPortableNBT`/`readFromPortableNBT` (no `super` call inside the portable methods -- they only write/read the tank's own state, not the TE id/coords).
- The 1.7.10 harvest lifecycle requires `removedByPlayer(willHarvest=true)` to return `true` WITHOUT removing the block, so `getDrops` (called from `harvestBlock` → `super.harvestBlock`) can read the still-present TE. `harvestBlock` then calls `setBlockToAir` which triggers `breakBlock` (drops upgrades for non-sealed, removes TE).
- Sealed break writes `"Tile"` portable NBT (fluid + attributes + upgrades + sealed + owner + securityKey + customName). Non-sealed break writes nothing → clean-slate item.
- Custom name always travels on the item's display tag (visible in inventory), regardless of sealed state.
- Creative upgrades (`ItemUpgradeCreative`) are NOT dropped on non-sealed break (matching SD 1.7.10 `BlockDrawers.breakBlock`).
