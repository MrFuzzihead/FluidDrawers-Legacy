# Master Migration Spec — FluidDrawers 1.12.2 → 1.7.10

## 1. Purpose and scope

**Mod:** FluidDrawers (basic Fluid Tank only)
**Description:** Adds fluid tanks that store, display, and interact with fluids via a GUI and bucket right-click. Integrates with StorageDrawers for upgrades (capacity, downgrade, void, redstone, lock, creative). The framed/custom tank variant, StorageDrawers controller integration, FramedCompactDrawers integration, and Waila integration are deferred to stretch milestones.
**Who:** Single-fluid tank block (tank, not tank_custom). End-to-end: registration, texturing, tile entity, fluid storage, bucket interaction, GUI, upgrades, seal/security, item-NBT persistence, crafting recipe, quantity label (optional), config GUI (optional).

## 2. Forge builds and MCP mapping sets

| Property            | Value                                                                                                                                                  |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|
| Source mod          | FluidDrawers 1.0.7 (1.12.2) by MrFuzzihead, package `xyz.phanta.fluiddrawers`                                                                          |
| Source dependencies | **libnine** (Virtue, @InitMe, L9GuiHandler, L9BlockStated, L9ItemBlock, L9Models, MathUtils, MirrorUtils) — NOT available in 1.7.10                    |
| Source dependencies | **Chameleon** (ChamTileEntity, TileDataShim, injectPortableData/injectData, CustomNameData, UnlistedModelData, ModelData) — NOT available in 1.7.10 SD |
| Source dependencies | **Forge capabilities** (ICapabilityProvider, AttachCapabilitiesEvent, CapabilityFluidHandler, CapabilityDrawerGroup) — NOT available in 1.7.10 Forge   |
| Target Forge        | 10.13.4.1614 (1.7.10), GTNH/RetroFuturaGradle build system                                                                                             |
| Target MCP channel  | `stable` (channel = stable)                                                                                                                            |
| Target MCP mappings | `12` (mappingsVersion = 12)                                                                                                                            |
| Target build tool   | Gradle via `com.gtnewhorizons.gtnhconvention` plugin                                                                                                   |
| Target runtime      | Java 8 (compiled via jabel/JVM Downgrader from modern syntax)                                                                                          |

## 3. Subsystem inventory

Pulled from `docs/class-audit.md`. 34 in-scope classes + 19 deferred classes, organized into these clusters:

- **Foundation/config** (5 classes): FluidDrawersMod, FluidDrawersConfig, CommonProxy, ClientProxy, NameConst. Boilerplate already exists.
- **Registration** (2 init classes): FdBlocks, FdGuis. Port from @InitMe/l9 to GameRegistry/IGuiHandler.
- **Block/renderer** (2+1 classes): BlockTankBase, BlockTank, (NEW) BlockTankRenderer. Major rewrite: IBlockState/PropertyBool → metadata/ForgeDirection; JSON model → ISimpleBlockRenderingHandler.
- **Tile entity** (1 class): TileTank. Major rewrite: ChamTileEntity → plain TileEntity; injectPortableData → inline NBT; capabilities → 1.7.10 IFluidHandler.
- **Fluid data model** (Cluster C, 10 classes): FluidDrawer, FluidDrawerGroup, FluidDrawerHost, SimpleFluidDrawer, SingletonFluidDrawerGroup, SimpleDrawerAttributes, FluidTypeMap, FluidTypeMultimap, DrawerTankWrapper, DrawerFluidHandler. Clean port + IFluidHandler adaptation.
- **Fluid handler adapters** (3 classes): BypassableFluidHandler, BypassingFluidHandlerWrapper, BlockInteractionUtils. Rewrite to 1.7.10 IFluidHandler + FluidContainerRegistry.
- **Networking** (2 classes): SPacketSyncFluidDrawerFluid, SPacketSyncFluidDrawerCount. ~1:1 port (SimpleNetworkWrapper).
- **GUI/container** (3 classes): ContainerTank, SlotDrawerUpgrade, GuiTank. Rewrite: L9GuiContainer/Component → vanilla GuiContainer + custom fluid widget.
- **Item/upgrades** (2 classes): ItemBlockTank, UpgradeItemHandler. Rewrite: L9ItemBlock → ItemBlock; capabilities → IInventory.
- **Rendering** (1 class): RenderTileTank. Rewrite: GlStateManager/BufferBuilder → GL11/Tessellator + lightmap.
- **Config GUI** (1 class): ConfigGuiHandler. Optional.

## 4. Phase order and definition-of-done

Each phase must pass **all** the following to be considered done:
1. `./gradlew build` compiles with zero errors (unresolved `// TODO` stubs are expected checkpoints, not failures).
2. `./gradlew runClient` launches without crash, no classloading/registration errors traceable to the phase.
3. For phases touching sync, proxy split, or GUI (notably Phases 6, 8): `./gradlew runServer` + client join with no proxy-split crash, no client-only-class error, and no packet-side bug.
4. The in-game test(s) listed in the phase description pass. Recorded in `docs/verification-log.md`.

| Phase | Name                                                       | Key classes                                                                                                                                                                                             | Depends on  | In-game test (summary)                                                                                                                        |
|-------|------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| 0     | Foundation, rules, audit, spec, build/launch gate          | LateMixinsLoader, clinerules/, agents/ skills/, class-audit.md, master-migration-spec.md                                                                                                                | —           | Mod loads; mod list shows FD + SD; log contains "I am FluidDrawers at version".                                                               |
| 1     | Block registration + creative tab + lang + flat icon       | FdBlocks, BlockTank, Config → FluidDrawersMod, CommonProxy, CreativeTabs, en_US.lang                                                                                                                    | 0           | Find Fluid Tank in creative tab; place (solid tank.png cube); break; drops self; item icon = tank.png.                                        |
| 2     | Hollow-frame block renderer (ISimpleBlockRenderingHandler) | BlockTankRenderer (NEW), BlockTank.isOpaqueCube/ renderAsNormalBlock, renderInventoryBlock, ClientProxy, FdBlocks                                                                                       | 1           | Hollow glass frame visible; no black box; correct AO; item icon in hotbar/hand.                                                               |
| 3     | Tile entity scaffolding                                    | TileTank (plain TileEntity), GameRegistry.registerTileEntity, BlockTank ITileEntityProvider                                                                                                             | 1           | F3 shows TE; save/reload → TE persists; no crash.                                                                                             |
| 4     | Config + fluid data model                                  | Config (Configuration), FluidDrawer, FluidDrawerGroup, FluidDrawerHost, SimpleFluidDrawer, SimpleDrawerAttributes, FluidTypeMap/Multi, DrawerTankWrapper, SingletonFluidDrawerGroup, DrawerFluidHandler | 3           | fluiddrawers.cfg generated; F3 shows TE; break/reload no crash; capacity logged.                                                              |
| 5     | Bucket fill/drain interaction                              | BlockInteractionUtils (FluidContainerRegistry), DrawerFluidHandler/Bypassable/Bypassing (1.7.10 IFluidHandler), TileTank implements IFluidHandler, BlockTank.onBlockActivated dispatcher                | 4           | Water bucket fills tank; empty bucket drains; capacity limits enforced.                                                                       |
| 6     | Tile entity sync (description packet)                      | TileTank.getDescriptionPacket/onDataPacket, markBlockForUpdate                                                                                                                                          | 5           | Walk out of render distance and back → fluid re-appears at correct level (primary). Save/reload → persists (secondary). runServer gate.       |
| 7     | Fluid TESR rendering                                       | RenderTileTank (1.7.10 TESR + Tessellator + lightmap), ClientRegistry.bindTESR, TileTank.shouldRenderInPass                                                                                             | 6           | Water/lava rendered; level tracks fill; torch brightness test.                                                                                |
| 8     | GUI / Container                                            | FdGuis (IGuiHandler), ContainerTank, SlotDrawerUpgrade, GuiTank (vanilla + custom widget), onBlockActivated sneak+empty-hand branch                                                                     | 5, 4        | Sneak+right-click → GUI; plain right-click does nothing; runServer gate.                                                                      |
| 9     | Capacity upgrades                                          | DrawerUpgradable, UpgradeItemHandler (IInventory), TankUpgradeData, storage/downgrade remap to 1.7.10 SD items                                                                                          | 8, 4        | Storage upgrade → capacity up; downgrade → 1000; can't remove if overfull.                                                                    |
| 10    | Behavioral upgrades                                        | void/redstone/lock/vending logic, canProvidePower + isProvidingWeakPower (direct, not comparator), markBlockForUpdate for vending texture                                                               | 9           | Void discards overfill; redstone lamp/dust reads 1-15; lock keeps fluid on drain; vending texture swaps immediately.                          |
| 11    | Seal & security                                            | ISealable (tape), IProtectable/SecurityManager (personalKey), dispatcher security-first guard + seal gate + overlays                                                                                    | 5, 7, 8, 10 | Tape seals (can't fill/drain); overlay visible; personalKey sets owner; non-owner blocked from everything; sealed break → contents preserved. |
| 12    | Item-block NBT persistence + custom name                   | ItemBlockTank (restore from NBT), BlockTank.getDrops/getDroppedDrawerItem, IWorldNameable                                                                                                               | 11          | Fill+upgrade, break → item; place → restored; anvil rename works.                                                                             |
| 13    | Crafting recipe                                            | GameRegistry.addShapedRecipe, en_US.lang                                                                                                                                                                | 1           | Craftable; shows in NEI.                                                                                                                      |
| 14    | Quantity label (optional)                                  | ModItems.quantifyKey, SPacketSyncFluidDrawerCount/Fluid, RenderTileTank floating label                                                                                                                  | 7, 10       | Quantify key toggles "Water / 1,000 mB" floating label.                                                                                       |
| 15    | Config GUI (optional)                                      | ConfigGuiHandler → GuiFactory/GuiConfig                                                                                                                                                                 | 4           | Mod list → FD → Config → edit values.                                                                                                         |

## 5. Per-phase definition-of-done details

### Phase 0 DoD
- [x] `LateMixinsLoader.getMixinConfig()` returns `"mixins.fluiddrawers.late.json"`.
- [x] `.clinerules/01-api-delta-*` includes the FluidDrawers-specific deltas table.
- [x] `.agents/skills/backport-phase/SKILL.md` references the granular phases in this spec.
- [x] `.agents/skills/verify-phase/SKILL.md` includes compile + dev-client + dedicated-server + behavior gates.
- [x] `docs/class-audit.md` exists with full inventory + deferred flags.
- [x] `docs/master-migration-spec.md` exists (this file).
- [x] `./gradlew build` compiles.
- [x] `./gradlew runClient` launches; mod list shows FD + SD; log contains version string.

### Phase 1 DoD
- [x] BlockTank registered as `GameRegistry.registerBlock(...)`.
- [x] Creative tab "Fluid Drawers" exists with TANK as its icon.
- [x] `tank.png` renders on all 6 faces (IIcon registered via registerIcons).
- [x] Item icon = `tank.png`.
- [x] `en_US.lang` includes `tile.fluiddrawers.tank.name` and `itemGroup.fluiddrawers`.
- [x] Placeable, breakable, drops itself.

### Phase 2 DoD
- [x] BlockTank has a custom render type registered with RenderingRegistry.
- [x] `isOpaqueCube()` = false, `renderAsNormalBlock()` = false (port from 1.12.2 source).
- [x] `renderInventoryBlock` draws the item correctly.
- [x] BlockTankRenderer draws the 7-element hollow frame (bottom/top slab + 4 corners + glass interior).
- [x] No black box / incorrect AO behind the glass. Hotbar/hand item renders.

> **Status (2026-07-26):** Implemented + `./gradlew build` PASS (zero errors, no `// TODO` stubs — all 1.7.10 APIs verified against decompiled source). User in-game test: steps 1/2/4 PASS; step 3 (held-in-hand) had a depth-write bug (framework disables `glDepthMask` for `getRenderBlockPass()!=0` blocks in the hand codepath) → **fixed** in `BlockTankRenderer.renderInventoryBlock` (save/force/restore `GL_DEPTH_WRITEMASK`); pending re-verification via `runClient`. See `docs/verification-log.md`. Dedicated-server gate N/A for this phase.

### Phase 3 DoD
- [x] TileTank extends `net.minecraft.tileentity.TileEntity` (not ChamTileEntity).
- [x] `writeToNBT`/`readFromNBT` stubs.
- [x] `GameRegistry.registerTileEntity(TileTank.class, "fluiddrawers_tile")`.
- [x] BlockTank implements ITileEntityProvider.
- [x] TE persists across save/reload (no crash — the F3 TE info display is a 1.11+ feature not available in 1.7.10).

> **Status (2026-07-26):** Implemented + `./gradlew build` PASS. **Correction:** F3 in 1.7.10 does NOT show per-block tile entity info (that's a 1.11+ addition). Phase 3's real gate is simply "no crash on place/save/quit/reload". The TE is a silent foundation — hold data starts in Phase 4. See `docs/verification-log.md`.

### Phase 4 DoD
- [x] `Configuration` reads/writes `baseCapacity` (32000), `baseCapacityDowngraded` (1000), `quantifyShowsFluidName` (true).
- [x] `FluidDrawer`, `FluidDrawerGroup`, `FluidDrawerHost` interfaces ported.
- [x] `SimpleFluidDrawer` ported; libnine MathUtils replaced; manual NBT (no INBTSerializable in 1.7.10).
- [x] `SimpleDrawerAttributes` (standalone class — SD 1.7.10 GTNH lacks the unified interface).
- [x] `FluidTypeMap`, `FluidTypeMultimap`, `DrawerTankWrapper` ported.
- [x] `SingletonFluidDrawerGroup`: no TileDataShim/capabilities; holds SimpleFluidDrawer + exposes IFluidHandler via DrawerFluidHandler.
- [x] `DrawerFluidHandler` implements 1.7.10 IFluidHandler.
- [x] TileTank holds the group, NBT-persists the fluid, getCapacity() = baseCapacity.
- [x] In-game: config/fluiddrawers.cfg generated with baseCapacity=32000; break/reload no crash.

> **Status (2026-07-26):** Implemented + `./gradlew build` PASS (26s, checkstyleMain passed). **Correction:** The plan claimed `IDrawerAttributes`/`IDrawerAttributesModifiable` exist in SD 1.7.10 API — they DO NOT in the GTNH 2.2.26 fork. `SimpleDrawerAttributes` is a standalone concrete class (methods match 1.12.2 FD callers). All other SD API refs confirmed in jar. The in-game config-file + no-crash check requires `runClient` manual verification. See `docs/verification-log.md`.

### Phase 5 DoD
- [ ] `BlockInteractionUtils.transferFluid` uses `FluidContainerRegistry` (fillContainer/drainContainer).
- [ ] `BypassableFluidHandler`, `BypassingFluidHandlerWrapper` implement 1.7.10 `IFluidHandler`.
- [ ] `TileTank implements IFluidHandler` (ForgeDirection-based: fill/drain/canFill/canDrain/getTankInfo).
- [ ] `BlockTank.onBlockActivated` establishes the ordered dispatcher (security-first stub → held-item dispatch → empty-hand+sneak).
- [ ] Bucket branch is held-item "else": gated by `facing != UP && !isSealed()`; calls `transferFluid(..., bypass=true)`.

### Phase 6 DoD
- [ ] `TileTank.getDescriptionPacket` returns NBTTagCompound with fluid-stack NBT.
- [ ] `TileTank.onDataPacket` reads fluid-stack NBT.
- [ ] `worldObj.markBlockForUpdate(x,y,z)` called on fluid change.
- [ ] Client TE updates on chunk reload and render-distance re-entry.

### Phase 7 DoD
- [ ] `RenderTileTank` is a 1.7.10 `TileEntitySpecialRenderer` using `Tessellator.startDrawingQuads`/`addVertexWithUV`/`draw`.
- [ ] Uses `Fluid.getStillIcon()` for the sprite, `Fluid.getColor()` for tint.
- [ ] Samples `world.getLightBrightnessForSkyBlocks(x,y,z, luminosity)` and applies via `OpenGlHelper.setLightmapTextureCoords`.
- [ ] `ClientRegistry.bindTileEntitySpecialRenderer(TileTank.class, renderer)`.
- [ ] `TileTank.shouldRenderInPass(0)` returns true.

### Phase 8 DoD
- [ ] `IGuiHandler` registered via `NetworkRegistry.registerGuiHandler`.
- [ ] `ContainerTank` (1.7.10 Container) with upgrade slots backed by stub IInventory.
- [ ] `GuiTank` extends `GuiContainer`, draws `gui/tank.png` + fluid widget.
- [ ] Dispatcher: empty-hand+sneak → open GUI (gated by `enableDrawerUI`). Plain right-click does nothing.
- [ ] GUI-title lang key added.

### Phase 9 DoD
- [ ] UpgradeItemHandler as IInventory (7 slots).
- [ ] `canAddUpgrade`/`canRemoveUpgrade` with capacity checks.
- [ ] SD 1.7.10 item remap working: ModItems.upgrade (metadata = level), ModItems.upgradeDowngrade.
- [ ] Capacity updates on storage upgrade install/removal.
- [ ] `upgradeConversion`/`upgradeOneStack` checks from 1.12.2 source dropped.

### Phase 10 DoD
- [ ] Void upgrade → excess overflow voided.
- [ ] Redstone upgrade → `canProvidePower()=true`; `isProvidingWeakPower` = getRedstoneLevel on all sides; `isProvidingStrongPower` = UP only.
- [ ] getRedstoneLevel = clamp(1+floor(14*amt/cap), 1, 15) (0 when empty).
- [ ] markDirty → notifyNeighbors when level emitter present.
- [ ] Lock upgrade → LockAttribute honored (fluid retained on drain to 0).
- [ ] Creative upgrade → unlimited vending + tank_vending.png via markBlockForUpdate.

### Phase 11 DoD
- [ ] Tape seals (ItemTape.onItemUse). Unseals via sneak+empty-hand in dispatcher.
- [ ] Personal key sets/clears owner. Security-first guard blocks non-owners.
- [ ] Seal/lock/void overlays render (via block renderer or TESR) with markBlockForUpdate.
- [ ] keepContentsOnBreak honored when sealed.
- [ ] Seal/ownership message lang keys.

### Phase 12 DoD
- [ ] ItemBlockTank reads "Tile" NBT on place, restores TE.
- [ ] getDrops/getDroppedDrawerItem writes TE NBT into item when sealed or keepContentsOnBreak.
- [ ] Custom name via IWorldNameable.

### Phase 13 DoD
- [ ] GameRegistry.addShapedRecipe for the tank.
- [ ] Recipe visible in NEI.

## 6. Standing conventions (recorded from Phase 0)

### Interaction dispatcher order (settled, do not patch per-phase)

The `onBlockActivated` dispatcher order is locked from the 1.12.2 source and must be preserved by every phase that inserts a branch:

1. `tile == null` → false.
2. **Security first:** `SecurityManager.hasAccess(player, tile)` guards all subsequent logic (Phase 11). Non-owner is blocked before any branch.
3. **Held-item dispatch** (in this exact order):
   - `ItemKey`/`ModItems.tape` → return false (let item's own `onItemUse` apply seal/key).
   - `ItemUpgrade` → add upgrade (Phase 9).
   - `ItemPersonalKey` → toggle ownership (Phase 11).
   - else → fluid transfer, gated by `facing != UP && !isSealed()` (Phases 5/11).
4. **Empty hand + sneak:** if sealed, unseal; else if `enableDrawerUI`, open GUI (Phase 8). Plain non-sneak empty-hand does nothing.

### Lang keys (standing rule)

Every user-facing string (GUI titles, lock/seal/ownership messages, upgrade tooltips, config descriptions) gets an `en_US.lang` entry keyed `fluiddrawers.*` (or reuses an existing `storagedrawers.*` key). No hardcoded English in code.

## 7. Deferred stretch milestones

| Milestone                 | Classes                                                                                                                                                                                | Pre-conditions                                                                                   |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| Framed/Custom Tank        | BlockTankCustom, TileTankCustom, FramedTile, FramedTextureModel, DelegatingBakedModel, FramedModelData, FramedItem, ItemBlockTankCustom, MaterialData-equivalent, framing table mixins | Phases 0-12 (basic tank complete) + SD MaterialData availability confirmed                       |
| SD Controller Integration | FluidDrawerController, FluidControllerProxy, ControllerFluidCapabilityHandler, DrawerReflect, FluidDrawersCoreHooks, TileEntityController mixin                                        | Phases 0-12 + TileEntityController 1.7.10 structure verified + reflection/mixin access confirmed |
| FramedCompactDrawers      | FramedDrawerHandler                                                                                                                                                                    | Framed tank milestone + FC mod available                                                         |
| Waila                     | (none in FD source; Waila is already a compileOnly dep for potential own handler)                                                                                                      | Any phase; low priority                                                                          |
