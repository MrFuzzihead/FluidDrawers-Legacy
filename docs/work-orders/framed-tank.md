# Work Order — Framed Fluid Tank (`tank_custom`)

## Source (1.12.2 originals)
- `xyz/phanta/fluiddrawers/block/BlockTankCustom.java`
- `xyz/phanta/fluiddrawers/tile/TileTankCustom.java`
- `xyz/phanta/fluiddrawers/item/block/ItemBlockTankCustom.java`
- `xyz/phanta/fluiddrawers/block/base/BlockTankBase.java` (reference)
- `xyz/phanta/fluiddrawers/tile/base/FramedTile.java`, `item/base/FramedItem.java` (interfaces)
- `xyz/phanta/fluiddrawers/client/model/FramedTextureModel.java`, `client/util/FramedModelData.java` (1.12.2 baked-model rendering)
- `assets/fluiddrawers/models/block/tank_custom_base.json` (authoritative trim geometry)
- 1.7.10 reference: FCD `block/BlockFramedCompactDrawer`, `tile/TileFramedCompactDrawer`,
  `item/ItemFramedCompactDrawer`, mixins `MixinTileEntityFramingTable` / `MixinContainerFramingTable`

## Dependent classes (breaks if this changes)
- `BlockTank` (base) — framed subclass calls its protected constructor + protected render helpers.
- `BlockTankRenderer` — `renderMetalFrame`/`renderGlass`/`drawBox`/`renderOverlays`/`U`/`FULL_BRIGHT`
  were widened from `private` to `protected` for reuse.
- `TileTank` (base) — `TileTankCustom` overrides `writeToPortableNBT`/`readFromPortableNBT`.
- `ItemBlockTank` (base) — `ItemBlockTankCustom` extends it.
- `ModBlocks` / `CommonProxy` / `ClientProxy` / `ModRecipes` — registration + recipe.
- `Mixins` (enum) — two new late mixins target SD framing table.
- `MixinTileEntityController` — framed tanks remain `INetworked` `FluidDrawerHost` so they join a
  controller's fluid network for free.

## API delta rulebook rows that apply
- `tiledata classes` — no 1.7.10 `MaterialData`; inline `MatS`/`MatT`/`MatF` ItemStacks in the tile
  (matches `ItemCustomDrawers.makeItemStack` NBT format).
- `1.12.2 rendering -> 1.7.10` — framed rendering done with an `ISimpleBlockRenderingHandler` +
  material `IIcon` resolution (`Block.getBlockFromItem(mat).getIcon(4, dmg)`); JSON baked models /
  `IExtendedBlockState` / `IUnlistedProperty` do not exist.
- `block registration` — `GameRegistry.registerBlock(TANK_CUSTOM, ItemBlockTankCustom.class,
  "tank_custom")` + `registerTileEntity(TileTankCustom, ...)`; no registry events.
- `SimpleNetworkWrapper` — unchanged (not needed for framed materials; survives in tile NBT).

## Acceptance criteria (what "done" looks like)
- `compileJava` passes (client `ISBRH`/item renderer included).
- Framed Fluid Tank is craftable (base tank wrapped in `stickWood`).
- Placing it creates a `TileTankCustom`; placing a framed stack restores `MatS`/`MatT`/`MatF` onto
  the tile.
- In-world + inventory: the frame renders the **side** material and the outer border renders the
  **trim** material; with no materials it shows the raw StorageDrawers `drawers_raw_side` texture
  (no dedicated "empty framed tank" texture yet).
- Framing table (SD): the framed tank is accepted in slot 0 (mixin), and with side material present
  the output slot produces a new framed-tank item carrying the selected materials (mixin).
- Breaking drops the framed tank preserving its materials (both unsealed top-level tags and inside
  the sealed `Tile` portable NBT).
- Tile entity registered under `fluiddrawers_tile_custom`; TESR bound so fluid renders inside the
  framed tank.
- Lang keys: `tile.fluiddrawers.tank_custom.name` + material tooltip keys.

## Status: DONE (core + trim + framing-table mixins + recipe). 
- **Z-fighting fix:** the framed frame is rendered inset by 0.05px (`BlockTankCustomRenderer.S`), so its
  outer faces sit inside the 1px trim ring instead of being coplanar with the trim pillars'/edge
  bars' outer faces. Also corrected the north/south face flags on the bottom/top edge bars (they
  were drawing their inward faces instead of the outward faces). Mirrors the 0.05 offsets in
  `tank_custom_base.json`.
- **Glass tint (front material):** the framing table's front slot (slot 3) only accepts glass
  blocks/panes (clear or stained) when the input is a framed tank. The front material colours the
  rendered window via its own block icon (`BlockTankCustomRenderer.resolveGlassIcon` ->
  `block.getIcon(4, meta)`), applied in-world and to the item icon. Clear glass/pane = plain glass;
  stained glass/pane = the baked-colour `glass_<color>` texture. Side/trim stay opaque-only
  (`MixinTileEntityFramingTable`).
  **IMPORTANT (1.7.10 quirk):** `Block.getRenderColor(meta)` returns white (0xFFFFFF) for stained
  glass -- the colour lives in the baked per-colour textures (`BlockStainedGlass.getIcon` returns
  `glass_<color>`; it does NOT override `getRenderColor`). Do NOT use `getRenderColor` for the
  window tint; resolve the material's own icon instead.
- **Glass-front is tank-exclusive:** `TileEntityFramingTable.isItemValidMaterial` is NOT extended,
  so glass is not a generic framing-table material and SD framed drawers are unaffected. Shift-click
  of glass into the tank's front slot is handled by a tank-scoped
  `MixinContainerFramingTable#fluiddrawers$shiftGlassToTankFront` injection on
  `ContainerFramingTable.transferStackInSlot` that merges glass only into the front slot (manual
  click placement goes through the tile's `isItemValidForSlot` mixin).
- **Mixin shadow constraint (crash fix):** the container mixin must NOT shadow inherited
  `Container` members -- `@Shadow` on `mergeItemStack`/`inventorySlots` (declared in `Container`,
  not `ContainerFramingTable`) fails at apply time with "was not located in the target class" and
  crashes the framing table (`NoClassDefFoundError: ...ContainerFramingTable`). The shift-click
  merge therefore uses only shadows of fields declared directly in `ContainerFramingTable`
  (`tableInventory`, `inputSlot`, `materialFrontSlot`, `playerSlots`, `hotbarSlots`) and performs
  the front-slot placement manually against the public `Slot`/`ItemStack` API.

Glass tint stretch originally planned as a follow-up is now implemented via the front material
instead of a separate dyed-tint field (chosen because it reuses the existing material/NBT path and
the vanilla stained-glass color lookup; a dye-based tint would need a new synced field + interaction).
