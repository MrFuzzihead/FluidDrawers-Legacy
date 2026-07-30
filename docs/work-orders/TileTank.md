# Work Order — TileTank (Phase 3)

**Phase:** 3 — Tile entity scaffolding
**Cluster:** B (tile)

## Source file(s)

- 1.12.2: `migrate/fluiddrawers/xyz/phanta/fluiddrawers/tile/TileTank.java` (extends `ChamTileEntity`, uses `injectPortableData`/`injectData`, `CustomNameData`, `UpgradeData`, `ControllerData`, `SingletonFluidDrawerGroup`, `TankAttributes`, `TankUpgradeData`).
- 1.7.10 (NEW):
  - `src/main/java/com/mrfuzzihead/fluiddrawers/tile/TileTank.java` — **NEW** plain `TileEntity` subclass.
  - `src/main/java/com/mrfuzzihead/fluiddrawers/block/BlockTank.java` — modified: `implements ITileEntityProvider`, `isBlockContainer = true`, `createNewTileEntity`, `breakBlock`, `onBlockEventReceived`.
  - `src/main/java/com/mrfuzzihead/fluiddrawers/CommonProxy.java` — modified: `GameRegistry.registerTileEntity(TileTank.class, "fluiddrawers_tile")` in `preInit`.

## Dependent classes (what breaks if this changes)

- `BlockTank.createNewTileEntity` must instantiate `TileTank` (import from `tile` package).
- `CommonProxy.preInit` must call `GameRegistry.registerTileEntity` BEFORE any block is placed (it already runs at mod init before world load).
- Future phases (4–12) will add fields and NBT persistence to `TileTank`; the stub `writeToNBT`/`readFromNBT` with `super` calls is the foundation they extend.

## API delta rulebook rows applied

- 1.12.2 `ChamTileEntity` → plain `net.minecraft.tileentity.TileEntity` (inlines all data; no `injectPortableData`/`injectData`).
- 1.12.2 `@RegisterTile("fluiddrawers")` → `GameRegistry.registerTileEntity(TileTank.class, "fluiddrawers_tile")` in `preInit`.
- 1.12.2 `ITileEntityProvider` (same in both versions — identical interface signature `createNewTileEntity(World, int)`).

## Acceptance criteria (Phase 3 DoD)

- [x] `TileTank` extends `net.minecraft.tileentity.TileEntity` (not `ChamTileEntity`).
- [x] `writeToNBT`/`readFromNBT` stubs (calls `super`; TODO for Phase 4 fields).
- [x] `GameRegistry.registerTileEntity(TileTank.class, "fluiddrawers_tile")` in `CommonProxy.preInit`.
- [x] `BlockTank implements ITileEntityProvider` with `createNewTileEntity`, `breakBlock`, `onBlockEventReceived`.
- [x] Compiles clean (`./gradlew build` BUILD SUCCESSFUL — verified below).
- [ ] **(manual, in-game)** Place tank → F3 over it shows a Fluid Drawers TileEntity.
- [ ] **(manual, in-game)** Save & quit → reload → block + TE still present; no crash.

## Verification notes

`ITileEntityProvider` interface verified from decompiled `build/rfg/minecraft-src`: `TileEntity createNewTileEntity(World worldIn, int meta)`.
`TileEntity.writeToNBT`/`readFromNBT` signatures verified (void, take NBTTagCompound).
`Block.isBlockContainer` field used by `Block.hasTileEntity(int)` — setting it true is the standard Forge 1.7.10 pattern (confirmed via SD `BlockTrimCustom`).
`GameRegistry.registerTileEntity` signature: `registerTileEntity(Class<? extends TileEntity>, String)` — confirmed from SD `ModBlocks`.
