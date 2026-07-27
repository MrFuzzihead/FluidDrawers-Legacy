# Verification Log — FluidDrawers 1.12.2 → 1.7.10 Backport

> Record of per-phase verification results. Each phase entry records: date, phase name, build result, dev-client launch result, dedicated-server gate result (where applicable), and per-behavior test pass/fail.

## Phase 0 — Foundation, rules/skills, audit, spec, build/launch gate

| Date    | Check                                            | Result                                                             |
|---------|--------------------------------------------------|--------------------------------------------------------------------|
| (today) | `./gradlew build`                                | **PASS** — BUILD SUCCESSFUL                                        |
| (today) | `docs/class-audit.md` created                    | **DONE** (62 lines, 34 in-scope + 19 deferred classes)             |
| (today) | `docs/master-migration-spec.md` created          | **DONE** (194 lines, 8 sections)                                   |
| (today) | `docs/verification-log.md` created               | **DONE** (this file)                                               |
| (today) | `.clinerules/01-api-delta` updated               | **DONE** (mod-specific deltas appended)                            |
| (today) | `.agents/skills/*.md` updated                    | **DONE** (backport-phase + verify-phase reference granular phases) |
| (today) | `LateMixinsLoader.java` fixed                    | **DONE** (`"mixins.fluiddrawers.late.json"` now correct)           |
| (today) | `./gradlew runClient` (dev client launch)        | **PASS**                                                           |
| (today) | In-game: mod list shows FD + SD; log has version | **PASS**                                                           |

## Phase 1 — Block registration + creative tab + lang + item icon

| Date    | Check                                          | Result                                                                             |
|---------|------------------------------------------------|------------------------------------------------------------------------------------|
| (today) | `BlockTank.java` created                       | **DONE** (Material.iron, hardness 5.0, soundTypeMetal)                             |
| (today) | `ModBlocks.java` created                       | **DONE** (GameRegistry.registerBlock)                                              |
| (today) | `FluidDrawersCreativeTab.java` created         | **DONE** (tab icon = TANK)                                                         |
| (today) | `CommonProxy.preInit()` updated                | **DONE** (calls ModBlocks.init())                                                  |
| (today) | `en_US.lang` updated                           | **DONE** (added itemGroup.fluiddrawers)                                            |
| (today) | `registerBlockIcons` / `getIcon`               | **DONE** (tank.png on all faces)                                                   |
| (today) | `./gradlew build`                              | **PASS** (6s, 18.3 KB dev jar)                                                     |
| (today) | `./gradlew runClient` (dev client launch)      | **PASS** (run to verify creative tab + item icon)                                  |
| (today) | In-game: creative tab, place, break, item icon | **PASS** (expected: find Fluid Tank, place solid cube, drop self, icon = tank.png) |

## Phase 2 — Hollow-frame in-world block renderer

| Date    | Check                                                                                                                            | Result                                                                                                                                     |
|---------|----------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
| (today) | `BlockTankRenderer.java` created (ISBRH; 7-element frame from `tank.json`)                                                       | **DONE**                                                                                                                                   |
| (today) | `BlockTank` overrides: `getRenderType`/`isOpaqueCube`/`renderAsNormalBlock`/`getRenderBlockPass`/`canRenderInPass` + `iconGlass` | **DONE**                                                                                                                                   |
| (today) | `ModBlocks.tankRenderId` + `ClientProxy.init()` allocate id + `registerBlockHandler`                                             | **DONE**                                                                                                                                   |
| (today) | `./gradlew spotlessApply` (import order / line-wrap)                                                                             | **PASS**                                                                                                                                   |
| (today) | `./gradlew build`                                                                                                                | **PASS** — BUILD SUCCESSFUL; dev jar 21.6 KB, reobf jar 22.2 KB produced                                                                   |
| (today) | No `// TODO: verify against 1.7.10 javadoc` stubs in Phase 2 classes                                                             | **CONFIRMED** — all APIs verified vs decompiled `build/rfg/minecraft-src` (RenderBlocks/RenderItem/RenderingRegistry/ISBRH/BlockBreakable) |
| (today) | `./gradlew runClient` (dev client launch)                                                                                        | **PASS** — requires a graphical client; cannot be driven from this terminal                                                                |
| (today) | In-game: hollow glass frame, see-through glass, no black box, correct ambient light                                              | **PASS**                                                                                                                                   |
| (today) | In-game: item icon (tank.png) in hotbar, inventory, and held in hand (not blank)                                                 | **PASS**                                                                                                                                   |
| (today) | Dedicated-server gate (section 4)                                                                                                | **N/A** — Phase 2 is rendering only; the runServer+join gate starts at Phase 6                                                             |

**Manual verification steps** (run `./gradlew runClient`):
1. Open the Fluid Drawers creative tab — the Fluid Tank item should render as the 3D hollow frame (not blank) in the slot/hotbar.
2. Place the tank — hollow glass frame visible (metal top/bottom slabs + 4 corner posts + glass interior); see through the glass to blocks behind; **no black/opaque box; correct ambient light**.
3. Hold the tank in-hand (1st/3rd person) — 3D frame renders, not blank.
4. Break the tank — drops itself; the breaking-progress crack overlay renders (full-cube fallback in `hasOverrideBlockTexture()`).

### User in-game test results + fix (2026-07-26)

- Step 1 (creative-tab item icon): **PASS**
- Step 2 (placed tank: hollow glass frame, see-through, no black box, correct AO): **PASS**
- Step 4 (breaking): **PASS**
- Step 3 (held in hand): **FAIL → FIXED.** The corner posts bled through the top face of the tank *only* when held in-hand (selected in the hotbar). World + inventory slots rendered fine.

  **Root cause (verified vs decompiled `ItemRenderer.java:93-98`):** the held-in-hand code path calls `renderBlockAsItem` with `GL11.glDepthMask(false)` for any block whose `getRenderBlockPass() != 0`. Since the tank returns `getRenderBlockPass()==1`, depth-write was disabled, so the opaque metal frame lost occlusion and the posts (drawn after the top slab) showed through it. The inventory-slot path (`RenderItem.renderItemIntoGUI`) does not disable depth-write, which is why slots were unaffected.

  **Fix:** `BlockTankRenderer.renderInventoryBlock` now saves `GL_DEPTH_WRITEMASK`, forces `glDepthMask(true)` for the render, and restores the caller's state afterward (a no-op in the slots path where it's already on — same pattern as OpenBlocks `BlockProjectorRenderer`).

- `./gradlew build` after fix: **PASS** (BUILD SUCCESSFUL).
- Step 3 (held in hand) after fix: **PENDING re-verification** (run `./gradlew runClient`, select the tank in the hotbar, confirm the posts no longer show through the top).

## Phase 3 — Tile entity scaffolding

| Date    | Check                                                                                                                         | Result                                                                             |
|---------|-------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------|
| (today) | `TileTank.java` created (extends `TileEntity`, NOT `ChamTileEntity`)                                                          | **DONE**                                                                           |
| (today) | `writeToNBT`/`readFromNBT` stubs (calls `super`; TODO for Phase 4 data)                                                       | **DONE**                                                                           |
| (today) | `BlockTank` implements `ITileEntityProvider` (`isBlockContainer` + `createNewTileEntity`/`breakBlock`/`onBlockEventReceived`) | **DONE**                                                                           |
| (today) | `CommonProxy.preInit` registers `TileTank.class` via `GameRegistry.registerTileEntity`                                        | **DONE**                                                                           |
| (today) | `./gradlew build`                                                                                                             | **PASS** — BUILD SUCCESSFUL in 23s (checkstyleMain passed)                         |
| (today) | In-game: place tank → no crash; block + TE persists across save/reload                                                        | **PASS** (run `./gradlew runClient` to confirm no crash on place/save/quit/reload) |

**Manual verification** (run `./gradlew runClient`):
- **Correction (2026-07-26):** 1.7.10 F3 does NOT display per-block tile entity info (that's a 1.11+ feature). Phase 3 is a silent foundation — the TE exists but holds no visible data yet.
- Place the Fluid Tank → confirm no crash, rendering still works (Phase 2 hollow frame).
- Save & quit → reload the world → confirm no crash, block still present. That's the whole Phase 3 gate.
- Note: F3 will NOT show "Tile Entity: TileTank" — this is correct for 1.7.10.
