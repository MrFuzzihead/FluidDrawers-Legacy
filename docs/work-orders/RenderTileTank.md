# Work Order — RenderTileTank (Phase 7)

**Phase:** 7 — Fluid TESR rendering (see the fluid inside)
**Cluster:** rendering (new class)

## Source file(s)

- 1.12.2: `migrate/fluiddrawers/xyz/phanta/fluiddrawers/client/tesr/RenderTileTank.java` (extends 1.12.2 `TileEntitySpecialRenderer<T>`, uses `GlStateManager`/`BufferBuilder`/`DefaultVertexFormats`, `FluidRenderUtils`, `TextFormatUtils`, libnine `MathUtils`, per-vertex `func_187314_a(lMapX, lMapY)` lightmap, plus a Phase-14 floating-label path).
- 1.7.10 (NEW / modified):
  - `src/main/java/com/mrfuzzihead/fluiddrawers/client/tesr/RenderTileTank.java` — **NEW** `@SideOnly(CLIENT)` `TileEntitySpecialRenderer`.
  - `src/main/java/com/mrfuzzihead/fluiddrawers/tile/TileTank.java` — added `shouldRenderInPass(int)` -> `pass == 0`.
  - `src/main/java/com/mrfuzzihead/fluiddrawers/ClientProxy.java` — `ClientRegistry.bindTileEntitySpecialRenderer(TileTank.class, new RenderTileTank())` in `init()`.

## Dependent classes (what breaks if this changes)

- `ClientProxy.init` must register the TESR client-side only (it is; `ClientRegistry` is client-only and `RenderTileTank` is `@SideOnly(CLIENT)`, so the dedicated server never loads it).
- `TileTank.getDrawerGroup().getFluidDrawer()` / `getStoredFluid()` / `getMaxCapacity()` and `getAttributes().isConcealed()`/`isUnlimitedVending()` must keep their current signatures (they do -- Phases 4/5).
- `TileTank.shouldRenderInPass(0)` gates when the TESR runs; Phase 11 overlays may extend this to pass 1.
- **Future phases:** Phase 10 vending (`isUnlimitedVending` -> full/opaque fluid, already handled here) and Phase 14 (the `isShowingQuantity()` floating label -- left as a TODO gate in `renderTileEntityAt`; will add `FontRenderer` + the `SPacketSyncFluidDrawerCount`/`Fluid` packets). Phase 11 seal/lock/void overlays render in the **static block mesh** (`BlockTankRenderer`), not this TESR.

## API delta rulebook rows applied

- 1.12.2 rendering -> 1.7.10: `GlStateManager`/`BufferBuilder`/`DefaultVertexFormats` -> `GL11` + `Tessellator.startDrawingQuads`/`addVertexWithUV`/`draw`; `TextureAtlasSprite` -> `IIcon`.
- 1.12.2 `TileEntitySpecialRenderer.render(tile,x,y,z,partialTicks,destroyStage,alpha)` (6-arg, generic) -> 1.7.10 `renderTileEntityAt(TileEntity,double,double,double,float)` (5-arg, non-generic).
- 1.12.2 `func_147499_a(TextureMap.field_110575_b)` -> `bindTexture(TextureMap.locationBlocksTexture)`.
- 1.12.2 `func_178459_a().func_175626_b(pos, luminosity)` -> `tile.getWorldObj().getLightBrightnessForSkyBlocks(xCoord, yCoord, zCoord, luminosity)`.
- 1.12.2 per-vertex `func_187314_a(lMapX, lMapY)` lightmap -> `OpenGlHelper.setLightmapTextureCoords(lightmapTexUnit, ambLight % 65536, ambLight / 65536)` (SD `TileEntityDrawersRenderer` pattern) **and** `Tessellator.setBrightness(ambLight)` (per-vertex, `RenderBlockFluid` pattern). Both produce identical (blockLight<<4, skyLight<<4) coords.
- 1.12.2 `FluidRenderUtils.prepareRender(fluid)` -> `fluid.getFluid().getStillIcon()` (returns `IIcon`; null-checked).
- 1.12.2 `fluid.getFluid().isLighterThanAir()` -> **1.7.10 `fluid.getFluid().getDensity() < 0`** (verified remap: 1.7.10 `Fluid` has no `isLighterThanAir()`; the Fluid javadoc states "negative density indicates that the fluid is lighter than air"). `isGaseous()`, `getColor(FluidStack)`, `getLuminosity(FluidStack)` exist as-is.
- 1.12.2 `TextFormatUtils.getComponent(col, 2/1/0)` -> `(col >> 16 & 0xFF)/(col >> 8 & 0xFF)/(col & 0xFF)` / 255.0F (OpenBlocks pattern).
- 1.12.2 libnine `MathUtils.clamp(x,0,1)` -> manual `Math.max(0.0F, Math.min(1.0F, x))`.
- `cpw.mods.fml.client.registry.ClientRegistry.bindTileEntitySpecialRenderer`.

## Acceptance criteria (Phase 7 DoD)

- [x] `RenderTileTank` is a 1.7.10 `TileEntitySpecialRenderer` using `Tessellator.startDrawingQuads`/`addVertexWithUV`/`draw`.
- [x] Uses `Fluid.getStillIcon()` for the sprite, `Fluid.getColor(FluidStack)` for tint.
- [x] Samples `world.getLightBrightnessForSkyBlocks(x,y,z, luminosity)` and applies via `OpenGlHelper.setLightmapTextureCoords` (+ `setBrightness` for per-vertex correctness).
- [x] `ClientRegistry.bindTileEntitySpecialRenderer(TileTank.class, renderer)` in `ClientProxy.init`.
- [x] `TileTank.shouldRenderInPass(0)` returns true.
- [x] Compiles clean (`./gradlew build` BUILD SUCCESSFUL -- verified, incl. `:checkstyleMain` + Spotless).
- [ ] **(manual, in-game)** Fill with water -> blue water visible inside, level rises with more buckets; drain -> level falls.
- [ ] **(manual, in-game)** Fill with lava -> different color/icon.
- [ ] **(manual, in-game)** Place a torch next to the tank and remove it -- fluid brightness tracks the change (not stuck black/over-bright). Tests the lightmap path.

## Verification notes

All 1.7.10 APIs verified against the decompiled source in `build/rfg/minecraft-src` (no guessing):

- `TileEntitySpecialRenderer.renderTileEntityAt(TileEntity,double,double,double,float)` (5-arg), `bindTexture(ResourceLocation)`, `func_147498_b()` (FontRenderer -- Phase 14).
- `Tessellator`: `instance` static, `startDrawingQuads()`, `addVertexWithUV(x,y,z,u,v)`, `setBrightness(int)` (sets `hasBrightness`; `draw()` enables the lightmap texcoord pointer only when `hasBrightness`), `setColorRGBA_F(r,g,b,a)`, `draw()`. Confirmed `addVertex` writes lightmap/color only when the corresponding `has*` flag is set.
- `OpenGlHelper.setLightmapTextureCoords(int texUnit, float x, float y)`, `lightmapTexUnit`, `lastBrightnessX`/`lastBrightnessY` (saved/restored).
- `World.getLightBrightnessForSkyBlocks(int,int,int,int)` is `@SideOnly(CLIENT)` and returns `(skyLight << 20) | (blockLight << 4)` -- so `% 65536` = blockLight<<4 and `/ 65536` = skyLight<<4 (matches SD's split and `setBrightness`'s short-pair read).
- `Fluid`: `getStillIcon()`, `getColor()`/`getColor(FluidStack)`, `getLuminosity()`/`getLuminosity(FluidStack)`, `isGaseous()`, `getDensity()` (no `isLighterThanAir()` -- remapped to `getDensity() < 0`).
- `IIcon.getInterpolatedU/V(double)`, `getMinU/MaxU/MinV/MaxV()` (vanilla `RenderBlockFluid` + OpenBlocks use these).
- `TextureMap.locationBlocksTexture` (OpenBlocks `TileEntityTankRenderer` binds it for fluid).
- `TileEntity.shouldRenderInPass(int)` is **not** `@SideOnly` and defaults to `pass == 0` -- overriding it in common `TileTank` is server-safe (OpenBlocks `TileEntityTank` does the same).
- Reference TESR patterns: SD `TileEntityDrawersRenderer` (lightmap via `setLightmapTextureCoords`), OpenBlocks `TileEntityTankRenderer` (fluid via `Tessellator` + `getStillIcon`/`getColor`, `GL11.glDisable(GL_LIGHTING)`), vanilla `RenderBlockFluid` (`setBrightness` + `addVertexWithUV`).

## Implementation notes / decisions

- **Geometry:** the plan summary said "inner box 0.375-0.625"; the 1.12.2 source is authoritative -- the fluid box is half-width `0.375` centered on `0.5` (x/z `0.125..0.875`), y `0.125..0.125+0.75*fill`. Implemented verbatim.
- **Lighting belt-and-suspenders:** the DoD names `OpenGlHelper.setLightmapTextureCoords`; that alone sets the lightmap texture-unit *current* coord (used when the Tessellator's lightmap array is disabled). To guarantee the vertex-array quads are lit regardless of prior GL state, `setBrightness(ambLight)` is also called per quad (bakes per-vertex lightmap). Both yield the same (blockLight<<4, skyLight<<4) coords, so they agree.
- **GL state:** blend on (SRC_ALPHA/ONE_MINUS_SRC_ALPHA), fixed-function lighting + cull off for the draw, then all restored (incl. `lastBrightnessX/Y`). Disabling lighting stops the tint being double-darkened; the lightmap (texture unit 1) still applies via the texture env, so torch-brightness tracking works.
- **Phase 14 label:** the `isShowingQuantity()` branch is a TODO gate -- not rendered in Phase 7.

