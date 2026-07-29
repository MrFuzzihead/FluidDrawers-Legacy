# FluidDrawers 1.12.2 → 1.7.10 Backport Plan (Basic Tank, end-to-end)

> Source: `migrate/fluiddrawers` (1.12.2 FluidDrawers 1.0.7 + libnine + Chameleon)
> Target: Forge 1.7.10 (build 10.13.4.1614, MCP stable_12), GTNH/RetroFuturaGradle
> Reference mods: `migrate/storagedrawers` (SD 1.7.10), `migrate/openblocks` (1.7.10 tank + fluid render)
> Status: **Approved scope** -- basic Fluid Tank end-to-end; Framed/Custom Tank + SD Controller integration deferred to stretch milestones.

## 1. Scope

**In scope:** The basic `tank` block -- a placeable, textured fluid tank that stores fluid (capacity from config), fills/drains via bucket right-click and via a GUI, renders the fluid inside, supports SD upgrades (capacity/downgrade/void/redstone/lock/creative), seal (tape) + security (personal key), preserves contents in the item NBT on break, and is craftable.

**Deferred to post-plan stretch milestones** (per approved scope decision):
- **Framed/Custom Tank** (`tank_custom`, `FramedTextureModel`, `DelegatingBakedModel`, `FramedModelData`, `TileTankCustom`, `FramedTile`, `FramedItem`, `ItemBlockTankCustom`, SD `MaterialData`, framing-table coremod hooks).
- **SD Controller integration** (`FluidDrawerController`, `FluidControllerProxy`, `ControllerFluidCapabilityHandler`, `DrawerReflect`, `FluidDrawersCoreHooks.updateFluidControllerCache`, the `TileEntityController` mixin) -- i.e. "right-click the SD controller to fill/drain all connected tanks" + the tank being on the controller network (`INetworked`/`ControllerData`).
- **FramedCompactDrawers** (`FramedDrawerHandler`) and **Waila** integration.
- The entire `coremod/` package (`FluidDrawersCoreMod`, `FluidDrawersClassTransformer`, `TransformClass*`) -- replaced conceptually by **Mixins** (boilerplate already set up), but no mixins are needed for the basic tank.

## 2. Key architectural deltas discovered (feed the rules/skills update in Phase 0)

The 1.12.2 source is layered on three things that **do not exist in 1.7.10** and must be replaced:

| 1.12.2 dependency                                                                                                                                                                                                   | 1.7.10 replacement                                                                                                                                                                                                                                                           |
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **libnine** (`Virtue`, `L9CreativeTab`, `@InitMe`, `L9GuiHandler`/`GuiIdentity`, `L9Models`/baked models, `MathUtils`, network/GUI registrars)                                                                      | Plain `@Mod` + `CreativeTabs` + `GameRegistry.registerBlock/registerItem/registerTileEntity` + `IGuiHandler`/`NetworkRegistry` + `IIcon` + `MathHelper`                                                                                                                      |
| **Chameleon** (`ChamTileEntity`, `TileDataShim`, `injectPortableData`/`injectData`, `CustomNameData`, `UnlistedModelData`, `ModelData`, `IconUtil`)                                                                 | Not available; SD 1.7.10 **inlines** tile-data in `TileEntityDrawers`. Reimplement the data inline in our `TileTank` with manual NBT delegation.                                                                                                                             |
| **Forge capabilities** (`AttachCapabilitiesEvent`, `ICapabilityProvider`, `CapabilityFluidHandler`, `CapabilityDrawerAttributes`, `CapabilityDrawerGroup`) + 1.12.2 `IFluidHandler`/`FluidUtil`/`FluidActionResult` | 1.7.10 `IFluidHandler` (`fill(ForgeDirection,FluidStack,boolean)` / `drain(ForgeDirection,FluidStack,boolean)` / `drain(ForgeDirection,int,boolean)` / `canFill`/`canDrain`/`getTankInfo`) + `FluidContainerRegistry` for buckets. (Reference: OpenBlocks `TileEntityTank`.) |

**StorageDrawers 1.7.10 API deltas** (the 1.12.2 FD imports map differently):
- `ItemUpgradeStorage`/`EnumUpgradeStorage` → **do not exist**; 1.7.10 SD uses one metadata-based `ModItems.upgrade` (level = item damage) + `StorageDrawers.config.getStorageUpgradeMultiplier(level)`.
- `ItemKey`, `ModItems.upgradeConversion`, `ModItems.upgradeOneStack` → **do not exist** in 1.7.10. Locking is via `ModItems.upgradeLock`; seal via `ModItems.tape`; security via `ModItems.personalKey`; quantity via `ModItems.quantifyKey`; shroud via `ModItems.shroudKey`.
- `UpgradeData`/`ControllerData`/`MaterialData`/`CustomNameData` tiledata classes → **do not exist** as separate classes; SD 1.7.10 inlines them. (We reimplement what we need inline.)
- `api.storage.*` (`IDrawer`, `IDrawerGroup`, `IDrawerAttributes`, `INetworked`, `LockAttribute`, `ISealable`, `IProtectable`, `ISecurityProvider`, `SecurityManager`) → **do exist** and port cleanly.
- SD 1.7.10 `TileEntityDrawers` implements its own `CapabilityProvider` interface (an SD-internal abstraction, **not** Forge capabilities) -- a lead to verify for the deferred controller milestone, not needed for the basic tank.

**Other deltas** (already partly in the rulebook, to be expanded): `IBlockState`/`PropertyBool`/`IExtendedBlockState`/`BlockRenderLayer`/`EnumFacing` → int metadata + `ForgeDirection` + `getRenderBlockPass`; JSON models/blockstates → `IIcon` + `ISimpleBlockRenderingHandler`/`RenderingRegistry`; JSON recipes → `GameRegistry.addRecipe`; loot tables → `getDrops`; `@Config` → `Configuration`; `@Mod.EventBusSubscriber` → explicit `EVENT_BUS.register(...)`; `ItemStack.EMPTY`/`isEmpty()` → `null`/`== null`; `net.minecraftforge.fml.*` → `cpw.mods.fml.*`; `GlStateManager`/`BufferBuilder`/`DefaultVertexFormats` → `GL11` + `Tessellator.startDrawingQuads`/`addVertexWithUV`; `BlockPos` → `int x,y,z`; coremod ASM → Mixins. SRG `func_...` names follow the "never guess" rule.

## 3. Pre-existing assets / scaffolding (already done)
- `gradle.properties` (GTNH/RetroFuturaGradle, Forge 10.13.4.1614, MCP stable_12, jabel, mixins on), `dependencies.gradle` (StorageDrawers `api`, Waila `compileOnly`, NEI runtime), boilerplate `FluidDrawers`/`CommonProxy`/`ClientProxy`/`Config`/mixin classes.
- Assets copied to `src/main/resources/assets/fluiddrawers/`: `lang/en_US.lang` (already 1.7.10-cased), `textures/blocks/tank.png`, `tank_vending.png`, `textures/gui/tank.png`.
- **Bug found:** `LateMixinsLoader.getMixinConfig()` returns `"mixins.fuzzitweaks.late.json"` (wrong name) → will crash on launch. Fixed in Phase 0.

## 4. Phase order (each phase: **builds** . **launches** . **in-game test**)

> Convention: every phase ends with `./gradlew build` (zero errors) + `./gradlew runClient` (clean launch) + the listed in-game test, recorded in `docs/verification-log.md`. Block + texture come first (Phases 1-2); functionality after.
>
> **Dedicated-server gate:** from Phase 6 (first network sync) onward, also run `./gradlew runServer` and join from a client at least once per phase that touches sync, the proxy split, or a GUI (notably Phases 6 and 8). Integrated-server testing won't catch proxy-split mistakes, client-only-class crashes when a GUI opens on a dedicated server, or packet-registration/side bugs.
>
> **Interaction dispatcher (settled now; do not patch per-phase).** Verified from 1.12.2 `BlockTankBase.onBlockActivated` -- the held-item/empty-hand logic converges on one method across Phases 5, 8, 9, 11, 14. Lock in this order now and have each phase insert its branch into its pre-decided slot (never reorder earlier checks): (1) `tile == null` → false; (2) **security first** -- `SecurityManager.hasAccess(player, tile)` guards the whole method, so a non-owner is blocked before any branch (Phase 11); (3) held-item dispatch in this order: `ItemKey`/`ModItems.tape` → return false (let the items' own `onItemUse` apply the seal/key -- **sealing is not done in `onBlockActivated`**; unsealing is, in step 4) → `ItemUpgrade` → add upgrade (Phase 9) → `ItemPersonalKey` → toggle ownership (Phase 11) → else fluid transfer, gated by `facing != UP && !isSealed()` (Phase 5/11); (4) empty hand + **sneak** → if sealed, unseal; else if `enableDrawerUI`, open GUI (Phase 8); plain non-sneak empty-hand does nothing. This is why a non-owner can never pop the GUI before ownership is tested, and why Phase 11 depends on Phases 5 and 8.
>
> **Lang keys (standing rule).** Every user-facing string -- GUI titles, lock/seal/ownership messages, upgrade tooltips, config descriptions -- gets an `en_US.lang` entry keyed `fluiddrawers.*` (or reuses an existing `storagedrawers.*` key). No hardcoded English in code. Phase 0 records this in `docs/master-migration-spec.md`; Phases 8/10/11 add their keys as they introduce the strings.
>
> **Optional fluid interop test.** The mod exposes a standard 1.7.10 `IFluidHandler` (no bucket-specific logic), so any pipe mod should "just work." Once Phase 5 lands, optionally validate fill/drain + `getTankInfo` + `ForgeDirection.UNKNOWN` with a fluid-pipe mod (BuildCraft/Thermal Expansion/EnderIO) in the dev env -- this exercises paths manual bucket-clicking never hits. Note: this requires adding that mod to `runtimeOnlyNonPublishable`; treat as recommended, not a hard gate, unless you choose to make it one.

### Phase 0 -- Foundation, rules/skills, audit, spec, build/launch gate
- **Goal:** Make the project reliably build+launch and put the planning docs/rules in place before any feature code.
- **Touches:** `.clinerules/01-api-delta-1.12.2-1.7.10.md` (add the deltas table above), `.agents/skills/backport-phase/SKILL.md` + `verify-phase/SKILL.md` (replace the generic 9-phase order with "follow `docs/master-migration-spec.md`'s granular phases; per-phase gate = build+launch+in-game test (+ runServer+join for sync/GUI phases, see section 4)"), `src/main/java/.../mixins/LateMixinsLoader.java` (fix `getMixinConfig()` → `"mixins.fluiddrawers.late.json"`). Create `docs/class-audit.md` (run `audit-classes`) and `docs/master-migration-spec.md` (scope, Forge builds, MCP sets, phase order 0-15, per-phase DoD, deferred stretch milestones, **the interaction-dispatcher order (section 4) and the lang-keys standing rule**).
- **In-game test:** Mods list shows "Fluid Drawers" + "Storage Drawers"; chat log contains `I am FluidDrawers at version ...`; no crash.
- **Depends on:** nothing.

### Phase 1 -- Block registration + creative tab + lang + item icon
- **Goal:** A placeable `tank` block you can find and place. In-world it renders as a solid cube using `tank.png` on all faces; item icon = `tank.png`.
- **Touches:** `init/FdBlocks` → `ModBlocks`/`GameRegistry.registerBlock` + `ItemBlock`; `block/BlockTank` (minimal `Block`, material **verify** -- default `Material.glass`, hardness 5.0, glass sound, `setUnlocalizedName("fluiddrawers.tank")`); `FluidDrawers`/`CommonProxy.preInit` registration; a `CreativeTabs` ("Fluid Drawers"); add `itemGroup.fluiddrawers=Fluid Drawers` to `en_US.lang`.
- **In-game test:** Find "Fluid Tank" in the Fluid Drawers creative tab; place it (solid `tank.png` cube); break it; it drops itself. Item icon shows `tank.png`.
- **Depends on:** 0.

### Phase 2 -- Hollow-frame in-world block renderer (the real 1.12.2 look)
- **Goal:** Replace the solid cube with the 1.12.2 frame geometry -- metal frame (`tank.png`) + glass pane interior -- via a 1.7.10 `ISimpleBlockRenderingHandler` registered with `RenderingRegistry`. Item icon stays `tank.png`.
- **Touches:** new `client/renderer/BlockTankRenderer` (`ISimpleBlockRenderingHandler`, draws the 7 elements from `models/block/tank.json`: bottom/top slabs, 4 corner posts, inner glass cube) using `IIcon`s (`tank.png` + vanilla glass); `BlockTank.getRenderType` returns the custom render ID; **`isOpaqueCube()`=false and `renderAsNormalBlock()`=false** (port the 1.12.2 `func_149662_c`/`func_149686_d` overrides -- otherwise the hollow frame is treated as a full solid cube for lighting/AO/culling, and you get a black box behind the glass); **implement `renderInventoryBlock` explicitly** (a custom render type removes the automatic flat-icon fallback, so the item would render blank in the hotbar/hand even though `tank.png` is correct in-world); `getRenderBlockPass`/`canRenderInPass`; `ClientProxy` register handler + `registerIcons`. Delete the now-unneeded 1.12.2 `assets/.../blockstates` & `models` JSON from `src/main/resources` if copied (they're ignored in 1.7.10).
- **In-game test:** Placed tank shows the hollow glass frame -- see through the glass to the back, **no black/opaque box, correct ambient light**; item icon shows `tank.png` **in the hotbar, inventory, and held in hand** (not blank).
- **Depends on:** 1. *(Block + texture now complete -- functionality begins.)*

### Phase 3 -- Tile entity scaffolding
- **Goal:** `tank` has a `TileTank` that persists empty NBT across save/reload.
- **Touches:** `tile/TileTank` extends plain `net.minecraft.tileentity.TileEntity` (NOT `ChamTileEntity`); `writeToNBT`/`readFromNBT` stubs; `GameRegistry.registerTileEntity`; `BlockTank` implements `ITileEntityProvider` (`createTileEntity`/`hasTileEntity`).
- **In-game test:** Place tank → F3 over it shows a Fluid Drawers TileEntity; save & quit → reload → block + TE still present; no crash.
- **Depends on:** 1.

### Phase 4 -- Config + fluid data model (internal backend)
- **Goal:** The tank internally holds a fluid drawer with the configured capacity; no interaction yet.
- **Touches:** `Config`/`FluidDrawersConfig` → `Configuration` (`baseCapacity=32000`, `baseCapacityDowngraded=1000`, `quantifyShowsFluidName=true`); port the pure model -- `drawers/FluidDrawer`, `FluidDrawerGroup` (extends `IDrawerGroup`), `FluidDrawerHost`, `SimpleFluidDrawer` (replace libnine `MathUtils` → `MathHelper.clamp_int`), `util/SimpleDrawerAttributes`, `util/FluidTypeMap`, `util/FluidTypeMultimap`, `util/DrawerTankWrapper` (`IFluidTank`); rewrite `drawers/SingletonFluidDrawerGroup` (drop `TileDataShim`/capabilities → plain class holding a `SimpleFluidDrawer` + a 1.7.10 `IFluidHandler` via `util/DrawerFluidHandler`); `TileTank` holds the group, NBT-persists the fluid, exposes `getCapacity()` = `baseCapacity` (multiplier 1 for now).
- **In-game test:** Place tank → `config/fluiddrawers.cfg` generated with `baseCapacity=32000`; F3 shows TE; break/reload → no crash; (dev) one chat log on first place: `Tank capacity: 32000 mB`.
- **Depends on:** 3.
### Phase 5 -- Bucket fill/drain interaction
- **Goal:** Right-click with a bucket fills/drains the tank (first visible fluid behavior; fluid not yet rendered).
- **Touches:** `util/BlockInteractionUtils.transferFluid` → 1.7.10 `FluidContainerRegistry` (`fillContainer`/`drainContainer`) + the tank's `IFluidHandler.fill/drain` (replaces 1.12.2 `FluidUtil.tryFillContainer`/`tryEmptyContainer`); `util/DrawerFluidHandler`, `BypassableFluidHandler`, `BypassingFluidHandlerWrapper` → 1.7.10 `IFluidHandler` (`ForgeDirection`); `TileTank implements IFluidHandler` delegating to the group handler; `BlockTank.onBlockActivated` -- **establishes the ordered dispatcher (see section 4)** with the bucket branch as the held-item "else" case, gated by `facing != UP && !tile.isSealed()` (seal stubbed false until Phase 11), calling `transferFluid(..., bypass=true)` (wraps in `BypassingFluidHandlerWrapper` so direct player clicks ignore lock/void rules).
- **In-game test:** Right-click with water bucket → bucket empties; right-click with empty bucket → water bucket returned; fill to capacity → further fill rejected; drain to empty → further drain rejected. Build + launch + no crash.
- **Depends on:** 4.

### Phase 6 -- Tile entity sync (fluid survives reload/chunk reload)
- **Goal:** Fluid changes propagate to the client so state survives chunk reload + save/reload (prerequisite for rendering).
- **Touches:** `TileTank.getDescriptionPacket`/`onDataPacket` (vanilla TE sync carrying the fluid-stack NBT) + `worldObj.markBlockForUpdate` on fluid change; `markDirty`.
- **In-game test:** **Primary (tests the new sync path):** fill the tank (phase 5), then walk far enough to unload the chunk and walk back -- the fluid re-appears at the correct level via `getDescriptionPacket`/`onDataPacket` (a broken sync path can't hide behind an NBT test). Secondary: save & quit → reload → right-click with empty bucket → still drains (confirms server-side NBT persistence, already largely covered by Phases 3-4). No crash either way. **Dedicated-server gate (see section 4): join the `runServer` instance -- sync must work client→server→client with no class-side crash.**
- **Depends on:** 5.

### Phase 7 -- Fluid TESR rendering (see the fluid inside)
- **Goal:** Render the stored fluid as a colored, level-scaled sprite inside the glass frame, matching 1.12.2 geometry (inner box 0.375-0.625, height 0-0.75*fill).
- **Touches:** `client/tesr/RenderTileTank` → 1.7.10 `TileEntitySpecialRenderer` using `Tessellator`/`GL11` (drop `GlStateManager`/`BufferBuilder`/`DefaultVertexFormats`); fluid still icon (`Fluid.getStillIcon()`) + `Fluid.getColor()`/`FluidStack.getFluid().getColor()`; **per-quad lighting via the lightmap** -- sample `world.getLightBrightnessForSkyBlocks(x,y,z, fluid.getFluid().getLuminosity(fluid))` and apply with `OpenGlHelper.setLightmapTextureCoords` (GL immediate-mode TESR quads do NOT inherit baked-mesh lighting; skipping this is the classic "fluid renders solid black" 1.7.10 bug -- the 1.12.2 source does this via `func_178459_a().func_175626_b(...)` → `func_187314_a(lMapX, lMapY)`); `ClientRegistry.bindTileEntitySpecialRenderer(TileTank.class, ...)`; `TileTank.shouldRenderInPass(0)`.
- **In-game test:** Fill with water → blue water visible inside, level rises with more buckets; drain → level falls; fill with lava → different color/icon; **place a torch next to the tank and remove it -- fluid brightness tracks the change (not stuck black/over-bright)**. Build + launch + no crash.
- **Depends on:** 6.

### Phase 8 -- GUI / Container
- **Goal:** **Sneak + right-click with empty hand** opens the tank GUI showing the fluid tank widget + upgrade slots (slots inert until Phase 9). (Verified from 1.12.2: GUI opens on sneak + empty hand when `StorageDrawers.config.cache.enableDrawerUI`, not plain empty-hand -- resolves the earlier "confirm open-GUI condition" flag.)
- **Touches:** `init/FdGuis` → `IGuiHandler` + `NetworkRegistry.registerGuiHandler`; `inventory/ContainerTank` (1.7.10 `Container`, upgrade slots backed by a stub `IInventory` on the TE) + `inventory/slot/SlotDrawerUpgrade`; `client/gui/GuiTank` → vanilla `GuiContainer` + a custom fluid-tank widget (draw fluid sprite + `gui/tank.png` overlay) -- replaces libnine `L9GuiContainer`/`GuiComponentFluidTank`; insert the **empty-hand+sneak → open GUI / unseal** branch into the dispatcher (see section 4), gated by `enableDrawerUI`. Add GUI-title lang key.
- **In-game test:** Sneak + right-click tank (empty hand) → GUI opens showing current fluid + level + 7 upgrade slots; close → no crash; fluid widget updates after filling/draining; **plain (non-sneak) right-click does nothing**. **Dedicated-server gate (see section 4): join the `runServer` instance and open the GUI -- must not crash from a client-only class on the server side.**
- **Depends on:** 5, 4.

### Phase 9 -- Capacity upgrades (storage multiplier + downgrade)
- **Goal:** Upgrade slots accept/reject the right SD upgrades and capacity responds.
- **Touches:** `drawers/DrawerUpgradable`; `util/UpgradeItemHandler` → `IInventory` with `canAddUpgrade`/`canRemoveUpgrade` (remap: `ModItems.upgrade` metadata = storage level → `StorageDrawers.config.getStorageUpgradeMultiplier(level)`; `ModItems.upgradeDowngrade` → `baseCapacityDowngraded`); capacity-acceptability checks; `TileTank.getStorageMultiplier`/`getModifiedBaseCapacity`/`getDowngradedBaseCapacity`; `TankUpgradeData` logic (drop 1.12.2 `upgradeConversion`/`upgradeOneStack` checks).
- **In-game test:** Open GUI → insert a storage upgrade → capacity increases (fill more buckets confirm); insert downgrade → capacity drops to 1000; can't remove a storage upgrade if current fluid exceeds the lower capacity. Build + launch + no crash.
- **Depends on:** 8, 4.

### Phase 10 -- Behavioral upgrades (void, redstone, vending/creative)
- **Goal:** Void, redstone-signal, and creative-vending upgrades take effect. *(Lock removed from Phase 10 — see finding in OpenFindings.txt; lock is an interaction item, not an upgrade-slot item, handled in Phase 11.)*
- **Touches:** void (`IVoidable`/`isVoid` → excess voided); **redstone -- direct output, not a comparator override** (verified from 1.12.2 `BlockTankBase`/`TileTank`): `canProvidePower()=true`; `isProvidingWeakPower` returns `tile.getRedstoneLevel()` on **all sides** when `hasLevelEmitter()` (redstone upgrade present + `enableRedstoneUpgrades`); `isProvidingStrongPower` returns that level only on `UP`, else 0; `getRedstoneLevel()` = `clamp(1 + floor(14 * amount / maxCapacity), 1, 15)` (0 when empty); `markDirty` → `notifyNeighbors()` (notify block + block below) when a level emitter is present so fluid changes update the signal; vending (`ModItems.upgradeCreative` → `isUnlimitedVending` → infinite fluid + `tank_vending.png` texture swap in the **static block renderer** -- **must call `worldObj.markBlockForUpdate(x,y,z)` on install/remove so the chunk re-renders**, since the frame geometry is baked into the static mesh and won't refresh from a TE data change alone, unlike the Phase 7 TESR).
- **In-game test:** Void upgrade → overfill is discarded; **redstone upgrade → a redstone lamp OR dust adjacent on a horizontal side reads 1-15 proportional to fill (0 when empty) -- this is direct weak power on all sides, NOT a comparator reading** (also note the historically-faithful quirk: it will energize adjacent hoppers/dust on every side, not just comparators); creative upgrade → infinite output + tank shows vending texture **immediately on install (no chunk reload needed)**. Build + launch + no crash.
- **Depends on:** 9.

### Phase 11 -- Seal, security, & lock (tape + personal key + lock key) + attribute overlays
- **Goal:** Tape seals the tank; personal key sets ownership; lock key toggles lock; overlays render (tape/seal/lock).
- **Touches:** lock (`ModItems.upgradeLock` → toggle `LockAttribute.LOCK_EMPTY/LOCK_POPULATED` via interaction in `onBlockActivated`, NOT via upgrade slots — this is a physical key you right-click with); `ISealable` (tape → `sealed`; **sealing is applied by `ItemTape.onItemUse`**, not in `onBlockActivated` -- the block returns false for `ModItems.tape` to let the item run; **unsealing** is the empty-hand+sneak branch in `onBlockActivated`), `IProtectable`/`SecurityManager` (`ModItems.personalKey` → owner; the **security-first guard** in the dispatcher (section 4) blocks non-owners from every interaction), NBT for `sealed`/`owner`/`securityKey` (all config-gated by `enableTape`/`enablePersonalUpgrades` like 1.12.2); wire the `!isSealed()` gate on the Phase 5 fluid-transfer branch and the `ItemPersonalKey` ownership-toggle branch into the dispatcher; render the seal/lock/void overlays (1.12.2 `seal_part`/`lock_part`/`void_part` submodels → extra quads in the block renderer or TESR) with a `markBlockForUpdate` so they appear without a chunk reload; `keepContentsOnBreak` interaction with sealed. Add seal/ownership/lock message lang keys.
- **In-game test:** Apply tape → sealed (can't fill/drain), tape overlay visible **immediately**; sneak+empty-hand → unseals; apply personal key → owner set, a second player can't interact (can't fill/drain/open GUI -- security-first guard); right-click with lock key → lock toggled, lock overlay visible; break a sealed tank → re-place → contents preserved.
- **Depends on:** 5, 7, 8, 10.

### Phase 12 -- Item-block NBT persistence + custom name
- **Goal:** Breaking a tank keeps its fluid/upgrades in the dropped item; placing restores them.
- **Touches:** `item/block/ItemBlockTank` (read "Tile" NBT on place → restore TE; `FramedItem`-style material handling skipped -- deferred); `BlockTank.getDrops`/`getDroppedDrawerItem` (write TE NBT into item when sealed or `keepContentsOnBreak`); custom name (`IWorldNameable`/`CustomNameData`-equivalent inline) + name display.
- **In-game test:** Fill tank + add an upgrade, break it → pick up the item; place it → fluid + upgrade restored (with `keepContentsOnBreak` on, or if sealed). Anvil-rename works.
- **Depends on:** 11.

### Phase 13 -- Crafting recipe
- **Goal:** Craft the tank in a crafting table.
- **Touches:** `tank.json` recipe → `GameRegistry.addShapedRecipe` (**flag: verify the 1.12.2 `recipes/tank.json` ingredients** and remap any SD items to 1.7.10 names). *Independent of phases 3-12 -- can be slotted earlier if desired.*
- **In-game test:** Craft the tank per the recipe → obtain the Fluid Tank item; **recipe also shows in NEI** (NEI is a runtime dep and how most players find it).
- **Depends on:** 1.

### Phase 14 -- Quantity label (quantify key) *(optional polish)*
- **Goal:** Quantify key toggles a floating "fluid name / X mB" label on the tank.
- **Touches:** `ModItems.quantifyKey` → `isShowingQuantity` attribute; port `network/SPacketSyncFluidDrawerCount` + `SPacketSyncFluidDrawerFluid` → `SimpleNetworkWrapper`/`IMessage` (count + fluid sync); `RenderTileTank` floating-label rendering (1.7.10 `FontRenderer`/`Tessellator`).
- **In-game test:** Apply quantify key → floating "Water / 1,000 mB" appears on the tank sides; updates on fill/drain.
- **Depends on:** 7, 10.

### Phase 15 -- Concealment key (concealment key) *(optional polish)*
- **Goal:** Concealment key toggles hiding the fluid.
- **Touches:** I don't know
- **In-game test:** Apply concealment key → fluid not visible when toggled on, visible when toggled off.
- **Depends on:** 7, 10.

### STRETCH GOALS
- Framed Tank
- Controller integration
- FCD compat
- Waila integration
- Test piping integration

## 5. Risks / "verify, don't guess" list (flagged for compile-driven discovery)
- **Material/SoundType mapping** for `BlockTank` (1.12.2 `Material.field_151573_f`, `SoundType.field_185852_e`) → confirm via decompiled 1.12.2 (default `Material.glass` + glass sound).
- **SD 1.7.10 upgrade level metadata** for "obsidian"/storage levels + `getStorageUpgradeMultiplier(level)` signature; `upgradeLock`/`upgradeVoid`/`upgradeRedstone`/`upgradeStatus`/`upgradeCreative` behavior + config-gating flags (`enableStorageUpgrades`, etc.).
- **Lock mechanism in 1.7.10**: no `ItemKey` -- confirm locking is via `upgradeLock` (attribute toggle) vs. key click.
- **GUI-open vs. bucket-interaction trigger** in `BlockTankBase.onBlockActivated` (read full method during Phase 8).
- **Crafting recipe ingredients** (`recipes/tank.json`).
- **1.7.10 fluid render API**: `Fluid.getStillIcon()` vs `FluidStack.getFluid().getIcon(...)`, `Fluid.getColor()`, `Fluid.getLuminosity(FluidStack)` existence.
- **Vanilla TE sync** vs. the custom `SPacketSync*` packets (Phase 6 uses vanilla `getDescriptionPacket`; Phase 14 ports the custom ones).
- **SRG `func_...` names**: per the standing rule, emit `// TODO: verify against 1.7.10 javadoc` rather than guessing.
- **Redstone behavior (verified for FD; verify SD 1.7.10 wiring).** FD 1.12.2 emits **direct weak power on all sides** (strong on UP only), `getRedstoneLevel` = `clamp(1+floor(14*amt/cap),1,15)` -- confirmed from source; this is **not** a comparator override. Historically faithful quirk: it energizes adjacent hoppers/dust on every side. Confirm the 1.7.10 SD redstone-upgrade config gate (`enableRedstoneUpgrades`) and the exact accessor names on SD 1.7.10's inlined upgrade data (`getRedstoneType()`/`getStorageMultiplier()` equivalents) during Phase 10 -- SD 1.7.10 inlines upgrades, so these may not exist verbatim.
- **Static-mesh re-render on TE-driven appearance changes.** Anything the `ISimpleBlockRenderingHandler` draws from TE state (vending texture swap Phase 10, seal/lock/void overlays Phase 11) needs an explicit `worldObj.markBlockForUpdate(x,y,z)` -- the static chunk mesh does not refresh from a TE data change alone (unlike the Phase 7 TESR).
- **TESR lighting.** Phase 7 must sample the lightmap (`getLightBrightnessForSkyBlocks` + `OpenGlHelper.setLightmapTextureCoords`); without it the fluid renders solid black.
- **Interaction dispatcher ordering.** Settled in section 4 from source; the risk is a later phase (8/9/11/14) reordering checks instead of inserting into its slot -- verify each phase preserves security-first ordering.
- **Dedicated-server gaps.** Proxy split / client-only GUI classes / packet side bugs only surface on `runServer`+join (see section 4 gate).

## 6. Rules & skills updates (done in Phase 0, as requested)
- `.clinerules/01-api-delta-1.12.2-1.7.10.md` -- append the mod-specific deltas table from section 2 (libnine/Chameleon/capabilities/SD-item-remap/fluid-API/block-state/coremod→mixins).
- `.agents/skills/backport-phase/SKILL.md` + `verify-phase/SKILL.md` -- point at `docs/master-migration-spec.md`'s granular phase list instead of the generic 9-phase order; make the **build + launch + in-game test** gate explicit per phase.
- `docs/class-audit.md` -- full class inventory with subsystem tags + deferred flags (generated by `audit-classes`).
- `docs/master-migration-spec.md` -- scope (basic tank), deferred milestones, Forge 1.7.10 build 10.13.4.1614 / MCP stable_12, source 1.12.2 FluidDrawers 1.0.7 (+libnine), phase order 0-15, per-phase DoD.

---

This is **16 granular phases (0-15)**, each independently buildable, launchable, and in-game testable, with block + texture (1-2) before any functionality (3+), and all non-StorageDrawers dependencies + the two complex features deferred to stretch milestones.
