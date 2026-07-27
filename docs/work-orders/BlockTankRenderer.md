# Work Order — BlockTankRenderer (Phase 2)

**Phase:** 2 — Hollow-frame in-world block renderer (the real 1.12.2 look)
**Cluster:** A (block/renderer)

## Source file(s)

- 1.12.2 (geometry source of truth): `migrate/fluiddrawers/assets/fluiddrawers/models/block/tank.json` (7 elements), `.../blockstates/tank.json`, `.../models/item/tank.json` (item model parents the block model).
- 1.7.10 (NEW / modified):
  - `src/main/java/com/mrfuzzihead/fluiddrawers/client/renderer/BlockTankRenderer.java` — **NEW** `ISimpleBlockRenderingHandler`.
  - `src/main/java/com/mrfuzzihead/fluiddrawers/block/BlockTank.java` — added `getRenderType`, `isOpaqueCube`, `renderAsNormalBlock`, `getRenderBlockPass`, `canRenderInPass`, `iconGlass`, `getIconTank/getIconGlass`.
  - `src/main/java/com/mrfuzzihead/fluiddrawers/init/ModBlocks.java` — added `tankRenderId`.
  - `src/main/java/com/mrfuzzihead/fluiddrawers/ClientProxy.java` — `init()` allocates the render id + registers the handler.

## Dependent classes (what breaks if this changes)

- `BlockTank.getRenderType()` must return the registered id; `registerBlockIcons` must register both icons.
- `ModBlocks.tankRenderId` must be allocated client-side (ClientProxy.init) before render time.
- `ClientProxy.init` must register the handler with the same id (`registerBlockHandler(id, handler)`).
- **Future phases:** `BlockTankRenderer` will be extended in Phase 10 (vending `tank_vending.png` swap — needs `worldObj.markBlockForUpdate` since the frame is baked into the static mesh) and Phase 11 (seal/lock/void overlay quads). The static-mesh re-render caveat in the plan applies to both.

## API delta rulebook rows applied

- 1.12.2 rendering → 1.7.10: JSON models/blockstates → `IIcon` + `ISimpleBlockRenderingHandler` / `RenderingRegistry`.
- `BlockRenderLayer` → `getRenderBlockPass()` int + `canRenderInPass(int)` (two-pass: metal pass 0, glass pass 1 via `ForgeHooksClient.getWorldRenderPass()`).
- `GlStateManager`/`BufferBuilder` → `GL11` + `Tessellator` / `RenderBlocks.renderFace*`.
- `TextureAtlasSprite` → `IIcon` (`fluiddrawers:tank`; vanilla glass via `minecraft:glass`).
- `BlockPos` → `int x, y, z` (`renderWorldBlock` signature).
- `cpw.mods.fml.*` (`ISimpleBlockRenderingHandler`, `RenderingRegistry`).

## Acceptance criteria (Phase 2 DoD)

- [x] `BlockTank` has a custom render type registered with `RenderingRegistry` (`getNextAvailableRenderId` + `registerBlockHandler` in `ClientProxy.init`).
- [x] `isOpaqueCube()` = false, `renderAsNormalBlock()` = false (ports 1.12.2 `func_149662_c`/`func_149686_d`).
- [x] `renderInventoryBlock` draws the item explicitly (manages `startDrawingQuads`/`draw` — the framework does NOT wrap the custom path; `shouldRender3DInInventory` = true; GL translate -0.5; full brightness).
- [x] `BlockTankRenderer` draws the 7-element hollow frame (2 slabs [6 faces] + 4 corner posts [4 side faces] + glass interior cube [4 side faces]) from `tank.json` geometry.
- [x] Compiles clean (`./gradlew build` BUILD SUCCESSFUL).
- [ ] **(manual, in-game)** Placed tank shows the hollow glass frame; see through the glass to the back; no black/opaque box; correct ambient light. *(User-verified PASS 2026-07-26.)*
- [ ] **(manual, in-game)** Item icon renders `tank.png` in hotbar, inventory, and held in hand (not blank). *(Slots/hotbar PASS; held-in-hand had a depth-write bug — fixed, pending re-verify.)*

## Post-test fix (2026-07-26)

Held-in-hand rendering showed the corner posts bleeding through the top slab (world + inventory slots were fine). Root cause: `ItemRenderer.renderItemInFirstPerson` calls `renderBlockAsItem` with `glDepthMask(false)` for any block with `getRenderBlockPass() != 0` (`ItemRenderer.java:93-98`). Since the tank returns pass 1, depth-write was off, so the opaque metal frame lost occlusion and later-drawn posts showed through the earlier top slab. Fix: `renderInventoryBlock` saves `GL_DEPTH_WRITEMASK`, forces `glDepthMask(true)` for the render, restores afterward (no-op in the slots path). Same pattern as OpenBlocks `BlockProjectorRenderer`. `./gradlew build` re-verified PASS.

## Verification notes

All 1.7.10 APIs were verified against the decompiled source in `build/rfg/minecraft-src` (no guessing):
`ISimpleBlockRenderingHandler` signatures, `RenderingRegistry.getNextAvailableRenderId/registerBlockHandler`,
`RenderBlocks.renderFace*(Block,double,double,double,IIcon)` with `enableAO=false` (uses caller brightness/color, no `blockAccess` access — safe for inventory), `RenderBlocks.renderBlockAsItem` custom dispatch is **bare** (no `startDrawingQuads`/`draw` wrapper) at line 8361, `RenderItem.renderItemIntoGUI` binds the atlas + sets up isometric view + blend for `getRenderBlockPass()>0`, `RenderBlocks.renderItemIn3d` → `shouldRender3DInInventory` gates the 3D path, `Block.getMixedBrightnessForBlock`, `ForgeHooksClient.getWorldRenderPass`, vanilla glass icon name (`BlockBreakable` registers `"glass"`).
