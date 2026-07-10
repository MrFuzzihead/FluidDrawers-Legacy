# FluidDrawers 1.12.2 → 1.7.10 Backport Plan

## Executive Summary

The decompiled bytecode for `StorageDrawers-2.2.26-GTNH` confirms the real 1.7.10 API: `TileEntityController`/`TileEntityDrawers` use a `Map<BlockCoord, StorageRecord>` network (built by BFS over blocks implementing `INetworked`) where each `StorageRecord.group` is a plain `IDrawerGroup` — the exact pattern the 1.12 addon relied on, just without capabilities. 

There is **no chameleon library and no unified `IDrawerAttributes`**; instead `TileEntityDrawers` directly implements separate marker interfaces (`ILockable`, `IVoidable`, `IShroudable`, `IQuantifiable`, `ISealable`, `IProtectable`, `IDowngradable`) and stores upgrades as a plain `ItemStack[]` field with helper methods.

StorageDrawers-GTNH already requires **GTNHLib** and uses Mixins — confirming both are proven, compatible techniques in this toolchain. With Mixins enabled, we can cleanly hook `TileEntityController` instead of hand-writing ASM.

## Technical Approach

### 1. Data Model & Core Fluid Storage (~30% effort)

**Files to create:**
- `com.mrfuzzihead.fluiddrawers.drawers.FluidDrawer` — single-fluid container (port from 1.12)
- `com.mrfuzzihead.fluiddrawers.drawers.FluidDrawerGroup` — implement `IDrawerGroup` + `INetworked`
- `com.mrfuzzihead.fluiddrawers.util.FluidTypeMap*` — fluid→metadata map (port as-is)
- `com.mrfuzzihead.fluiddrawers.util.DrawerFluidHandler` — implement `net.minecraftforge.fluids.IFluidHandler`
- `com.mrfuzzihead.fluiddrawers.util.DrawerTankWrapper` — legacy `IFluidTank` adapter

**Key differences from 1.12:**
- No `INBTSerializable`/capability plumbing: use plain `readFromNBT`/`writeToNBT` on tiles
- Use legacy `net.minecraftforge.fluids.IFluidHandler` interface (`fill`/`drain`/`canFill`/`canDrain` with `ForgeDirection`) instead of capability-based `IFluidHandler`
- Implement `ILockable`/`ISealable`/`IProtectable`/`IShroudable`/`IQuantifiable` directly on `TileTank` (StorageDrawers' own interfaces) instead of porting Chameleon's `IDrawerAttributes`
- Store upgrades as `ItemStack[]` with manual getter/setter helpers, not an `UpgradeData` wrapper

---

### 2. Tile Entity & Block (~25% effort)

**Files to create:**
- `com.mrfuzzihead.fluiddrawers.tile.TileTank` — extends `TileEntity`, implements `IDrawerGroup` + `INetworked` + `IFluidHandler` + attribute interfaces
- `com.mrfuzzihead.fluiddrawers.block.BlockTank` — extends `BlockContainer`, manages tank placement/interaction

**Design:**
- `TileTank` is itself a drawer group (single drawer slot), directly discoverable by `TileEntityController.updateCache()`
- Implements `INetworked` so controller's BFS finds it
- Implements `ILockable`, `ISealable`, `IProtectable`, `IShroudable`, `IQuantifiable` directly (no wrapper class)
- Stores one `FluidStack fluid`, one `ItemStack[] upgrades` array, material items for framing (deferred), and state flags (sealed, voiding, vending, converting, etc.)
- `BlockTank.onBlockActivated()` opens a GUI or runs legacy fluid interaction helpers
- `BlockTank.breakBlock()` handles spill-on-break logic (reference: `BlockTankBase.onBlockDestroyedByExplosion` in migrate/)

**Reference implementations:**
- `TileTank` modeled on `TileEntityDrawers` (without Chameleon wrapper)
- `BlockTank` modeled on `BlockDrawers` (texture + rendering setup)

---

### 3. Controller Integration via Mixin (~20% effort)

**Files to create:**
- `com.mrfuzzihead.fluiddrawers.mixin.MixinTileEntityController` — Mixin class targeting `com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityController`

**Mixin logic:**
- `@Shadow` the private `storage` map to access `StorageRecord` instances
- `@Inject` at the end of `updateCache()` to scan the populated `storage` map
- Filter `StorageRecord.group instanceof FluidDrawerGroup` and collect them
- Build a `FluidDrawerController` cache (ported from 1.12, wraps multiple fluid drawers into one `IFluidHandler`)
- Use soft-`@Implements`/`@Interface` annotation to make `TileEntityController` implement `IFluidHandler`, delegating to that cache
  - This is the 1.7.10 equivalent of the 1.12 `AttachCapabilitiesEvent` approach
  - When pipes/other mods call `instanceof IFluidHandler` on a controller, it now reports true

**Key points:**
- No StorageDrawers source changes required — pure Mixin-based extension
- Pattern matches GTNHLib + GTNHCore's existing Mixin usage on 1.7.10
- Avoids complex ASM by using Mixin's bytecode transformation API

**Register in:**
- `gradle.properties`: ensure `usesMixins = true` (already enabled per user)
- `build.gradle.kts`: configure Mixin AP + JAR manifest reference
- `src/main/resources/META-INF/mods.toml` or `src/main/resources/mixins.fluiddrawers.json` (or equivalent FML-era config): declare mixin config class

---

### 4. GUI, Container & Network (~20% effort)

**Files to create:**
- `com.mrfuzzihead.fluiddrawers.inventory.ContainerTank` — extends `Container`, manages 7 upgrade slots (modeled on `ContainerDrawers`)
- `com.mrfuzzihead.fluiddrawers.client.gui.GuiTank` — extends `GuiContainer`, draws fluid level + upgrade slots
- `com.mrfuzzihead.fluiddrawers.network.SPacketSyncFluidDrawerFluid` — IMessage packet (x, y, z, fluid NBT)
- `com.mrfuzzihead.fluiddrawers.network.SPacketSyncFluidDrawerCount` — IMessage packet (x, y, z, count)
- `com.mrfuzzihead.fluiddrawers.core.handlers.GuiHandler` — implements `IGuiHandler`, registered via `NetworkRegistry.registerGuiHandler()`

**Design:**
- `ContainerTank` slots: player inventory (0–26) + tank upgrade slots (27–33)
- `GuiTank` renders a tall fluid tank graphic + upgrade slot textures (copied from `migrate/fluiddrawers/assets/…/gui/…`)
- Packets sent server→client on fluid change (by server, in `TileTank.onFluidChanged()`)
- Reuses StorageDrawers' own `SimpleNetworkWrapper` instance or creates a separate one

**Reference:**
- `ContainerDrawers`, `SlotUpgrade` from decompiled StorageDrawers
- Vanilla 1.7.10 `Container` + `GuiContainer` patterns
- `CountUpdateMessage` from StorageDrawers as a template for packet structure

---

### 5. Rendering (~15% effort)

**Files to create:**
- `com.mrfuzzihead.fluiddrawers.client.renderer.RenderTileTank` — extends `TileEntitySpecialRenderer<TileTank>`
- `com.mrfuzzihead.fluiddrawers.client.util.FluidRenderUtils` — helper to get texture & color from `FluidStack`

**Design:**
- `RenderTileTank.renderTileEntityAt()` calls `Tessellator.instance()` and `BufferBuilder` equivalents (direct immediate-mode in 1.7.10)
- Draws a 3D fluid box inside the tank model (similar to 1.12 but using old Tessellator API)
- If `showQuantity` is true, renders 2D text overlay (fluid name + volume in mB)
- References the 1.12 `RenderTileTank` for geometry logic, adapts to 1.7.10 GL/Tessellator calls

**`BlockTank` textures:**
- 6 textures per side (top, bottom, N, S, E, W) modeled on `BlockDrawers` setup
- Registered in `BlockTank.registerBlockIcons(IIconRegister)` (no JSON blockstates on 1.7.10)
- Reference: `BlockController.registerBlockIcons()` in decompiled StorageDrawers

---

### 6. Boilerplate Integration (~10% effort)

**Files to modify:**
- `com.mrfuzzihead.fluiddrawers.FluidDrawers` (main mod class)
- `com.mrfuzzihead.fluiddrawers.CommonProxy`
- `com.mrfuzzihead.fluiddrawers.ClientProxy`
- `com.mrfuzzihead.fluiddrawers.Config`

**Changes:**
- Register `BlockTank` + `ItemBlockTank` in preInit
- Register `TileTank` tile entity via `GameRegistry.registerTileEntity()`
- Register renderer in `ClientProxy` via `ClientRegistry.bindTileEntitySpecialRenderer()`
- Register GUI handler via `NetworkRegistry.INSTANCE.registerGuiHandler()`
- Register network packets (both sync packets) via `SimpleNetworkWrapper.registerMessage()`
- Load config values (base capacity, upgrade multipliers, etc.) from `Config`
- Copy language files + textures from `migrate/fluiddrawers/assets/…` to `src/main/resources/assets/fluiddrawers/…`
- Replace `recipes/tank.json` (1.12 Forge recipe format) with a Java `ShapedOreRecipe` in `ModRecipes` or an event handler

---

## Deferred / Out of Scope

1. **`BlockTankCustom` / Framed Drawer Skinning** — complex 1.12 feature requiring retexturing on-the-fly; defer to post-MVP
2. **Addon: Framed Compact Drawers Integration** — requires decompiled addon mod; skip for now
3. **Coremod / ASM patches** — superseded by Mixin approach

---

## Dependency & Build Configuration Checklist

- [ ] `StorageDrawers-2.2.26-GTNH:dev` already in `dependencies.gradle` ✓
- [ ] Mixins enabled in `gradle.properties` ✓
- [ ] `build.gradle.kts`: Add Mixin AP + annotation processor configuration
- [ ] `build.gradle.kts`: Ensure JAR manifest includes Mixin config JSON reference
- [ ] `src/main/resources/META-INF/…/fluiddrawers-mixins.json` (Mixin config)
- [ ] `src/main/java/com/mrfuzzihead/fluiddrawers/mixins/FluidDrawersMixinConfig.java` (Mixin config entry point)
- [ ] Confirm `gtnhlib` is available (already a transitive dep via StorageDrawers-GTNH)

---

## File Structure

```
src/main/java/com/mrfuzzihead/fluiddrawers/
├── FluidDrawers.java (main mod class)
├── CommonProxy.java
├── ClientProxy.java
├── Config.java
├── drawers/
│   ├── FluidDrawer.java
│   ├── FluidDrawerGroup.java
│   ├── FluidDrawerController.java (for mixin cache)
│   └── ... (utilities)
├── util/
│   ├── FluidTypeMap.java
│   ├── FluidTypeMultimap.java
│   ├── DrawerFluidHandler.java
│   ├── DrawerTankWrapper.java
│   └── ...
├── tile/
│   └── TileTank.java
├── block/
│   └── BlockTank.java
├── inventory/
│   ├── ContainerTank.java
│   └── SlotDrawerUpgrade.java
├── network/
│   ├── SPacketSyncFluidDrawerFluid.java
│   ├── SPacketSyncFluidDrawerCount.java
│   └── handlers/ (message handlers)
├── client/
│   ├── gui/
│   │   └── GuiTank.java
│   ├── renderer/
│   │   └── RenderTileTank.java
│   └── util/
│       └── FluidRenderUtils.java
├── core/
│   └── handlers/
│       └── GuiHandler.java
└── mixin/
    ├── FluidDrawersMixinConfig.java
    └── MixinTileEntityController.java

src/main/resources/
├── assets/fluiddrawers/
│   ├── textures/
│   │   ├── blocks/
│   │   │   └── (tank textures from migrate/)
│   │   └── gui/
│   │       └── (GUI textures from migrate/)
│   ├── lang/
│   │   ├── en_us.lang
│   │   └── (other langs from migrate/)
│   └── recipes/ (optional, recipes handled in code)
└── META-INF/
    └── fluiddrawers-mixins.json
```

---

## Implementation Order (Suggested)

1. **Phase 1: Core & Mixin (~Days 1–2)**
   - Implement `FluidDrawer`, `FluidDrawerGroup`, `FluidTypeMap`
   - Implement `TileTank` (entity only, no GUI yet)
   - Implement Mixin + test controller integration
   - Register tile entity

2. **Phase 2: Block & Interaction (~Day 3)**
   - Implement `BlockTank`
   - Port `BlockTankBase.onBlockActivated()` logic
   - Test block placement + controller discovery

3. **Phase 3: GUI & Network (~Days 4–5)**
   - Implement `ContainerTank`, `GuiTank`, sync packets
   - Register GUI handler + network
   - Test tank opening + upgrade placement

4. **Phase 4: Rendering (~Day 6)**
   - Implement `RenderTileTank`
   - Port textures + icons
   - Register renderer in `ClientProxy`

5. **Phase 5: Polish & Integration (~Day 7)**
   - Wire all into boilerplate (`FluidDrawers.java`, proxies, config)
   - Copy lang files + textures
   - Recipe registration
   - Test end-to-end

---

## Risk Mitigation

- **Risk: Mixin doesn't compile or apply** → Fallback to hand-written ASM coremod using RFG (Recaf/RetroGuard friendly) tooling if Mixin fails; GTNH ecosystem has proven ASM examples
- **Risk: `IFluidHandler` incompatible with pipes** → Confirm with test pipes early; 1.7.10 standard is `IFluidHandler` + `IFluidTank`
- **Risk: Texture registration fails** → Fallback to pre-baked sprite sheet if dynamic icon registration breaks
- **Risk: Network sync out of phase** → Use server-side authoritative state; client only renders server NBT data

---

## Success Criteria

- [ ] Tank block placeable in world
- [ ] Tank discoverable by `TileEntityController` (appears in controller's neighbor list)
- [ ] Fluid can be inserted into tank via controller (pipes, buckets, etc.)
- [ ] Fluid can be extracted from tank via controller
- [ ] GUI opens on right-click, shows fluid level
- [ ] Upgrades can be installed and affect tank behavior
- [ ] Liquid spills on tank breakage (if not sealed)
- [ ] Sealed/voided/vending/converting attributes toggle correctly
- [ ] No mod incompatibilities (StorageDrawers, GTNHLib, Forge)


