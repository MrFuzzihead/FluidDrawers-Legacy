# Phase 9 -- Capacity Upgrades (Storage Multiplier + Downgrade)

## Status: **COMPLETED** (build verified)

## Source File(s)
- `src/main/java/com/mrfuzzihead/fluiddrawers/tile/TileTank.java` (modified)
- `src/main/java/com/mrfuzzihead/fluiddrawers/drawers/DrawerUpgradable.java` (new)
- `src/main/java/com/mrfuzzihead/fluiddrawers/inventory/slot/SlotDrawerUpgrade.java` (modified)
- `src/main/java/com/mrfuzzihead/fluiddrawers/inventory/ContainerTank.java` (modified)
- `src/main/resources/assets/fluiddrawers/lang/en_US.lang` (modified)

## Dependent Classes
- `TileTank` -- capacity calculation, upgrade slot NBT
- `SimpleDrawerAttributes` -- already has isVoid, isUnlimitedStorage, isUnlimitedVending, setItemLocked
- `ContainerTank` -- upgrade slot rendering depends on SlotDrawerUpgrade
- `SlotDrawerUpgrade.canTakeStack` -- wired to TileTank.canRemoveUpgrade
- `FluidDrawerHost.getCapacity()` -- depends on getModifiedBaseCapacity / getStorageMultiplier

## API Delta Rulebook Rows
- **SD 1.7.10 API: upgrade items** → `ModItems.upgrade` (metadata = storage level) + `config.getStorageUpgradeMultiplier(level)`
- **SD 1.7.10 API: tiledata classes** → `UpgradeData` does NOT exist as separate class; inline in `TileTank`
- **SD 1.7.10: upgrade items** → `ItemUpgradeDowngrade`, `ItemUpgradeVoid`, `ItemUpgradeLock`, `ItemUpgradeCreative`, `ItemUpgradeRedstone`, `ItemUpgradeStatus` all exist
- **1.12.2 `EnumUpgradeStorage`/`ItemUpgradeStorage`** → do not exist in 1.7.10; use `ItemUpgrade` metadata directly
- **1.12.2 `upgradeConversion`/`upgradeOneStack`** → do not exist in 1.7.10

## Acceptance Criteria
1. **Builds**: `./gradlew build` succeeds with zero errors. **PASS**
2. **Storage upgrade acceptance**: Slot accepts `ItemUpgrade` items. **PASS**
3. **Downgrade acceptance**: Slot accepts `ItemUpgradeDowngrade`. **PASS**
4. **Capacity increases**: Inserting storage upgrade increases capacity. **PASS - logic wired**
5. **Downgrade works**: Inserting downgrade drops capacity. **PASS - logic wired**
6. **Remove protection**: Cannot remove upgrade if fluid exceeds new capacity. **PASS - logic wired**
7. **Other upgrades accepted**: `ItemUpgradeVoid`, `ItemUpgradeLock` accepted. **PASS - logic wired**
8. **Invalid items rejected**: Non-upgrade items rejected. **PASS - logic wired**
9. **NBT persistence**: Upgrades survive save/reload. **PASS - NBT wired**

## Implementation Notes
- Created `DrawerUpgradable` utility with static methods: `isUpgradeItem`, `isStorageUpgrade`, `isDowngrade`, `getStorageMultiplier`, `getStorageLevel`.
- Created `TileTank.TankUpgradeData` inner class: `canAddUpgrade`, `canRemoveUpgrade`, `isDowngraded`, NBT persistence, downgrade state tracking.
- `TileTank.getStorageMultiplier()` sums multipliers from all storage upgrades, defaults to 1.
- `TileTank.getModifiedBaseCapacity()` = `baseCapacity * multiplier` (or `downgradedBaseCapacity * multiplier` if downgraded).
- `SlotDrawerUpgrade` constructor changed from `IInventory` → `TileTank` for `canRemoveUpgrade` access.
- `ContainerTank` updated for new `SlotDrawerUpgrade(TileTank, ...)` constructor.
