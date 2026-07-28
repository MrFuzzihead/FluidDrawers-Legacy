# Work Order: Phase 8 — GUI / Container

## Source files (1.12.2)
- `migrate/fluiddrawers/xyz/phanta/fluiddrawers/init/FdGuis.java`
- `migrate/fluiddrawers/xyz/phanta/fluiddrawers/inventory/ContainerTank.java`
- `migrate/fluiddrawers/xyz/phanta/fluiddrawers/client/gui/GuiTank.java`
- `migrate/fluiddrawers/xyz/phanta/fluiddrawers/inventory/slot/SlotDrawerUpgrade.java`
- `migrate/fluiddrawers/xyz/phanta/fluiddrawers/util/UpgradeItemHandler.java`

## Target files (1.7.10)
- `src/main/java/com/mrfuzzihead/fluiddrawers/init/FdGuis.java` — **NEW**
- `src/main/java/com/mrfuzzihead/fluiddrawers/inventory/ContainerTank.java` — **NEW**
- `src/main/java/com/mrfuzzihead/fluiddrawers/client/gui/GuiTank.java` — **NEW**
- `src/main/java/com/mrfuzzihead/fluiddrawers/inventory/slot/SlotDrawerUpgrade.java` — **NEW**
- `src/main/java/com/mrfuzzihead/fluiddrawers/block/BlockTank.java` — **MODIFIED** (GUI dispatch branch)
- `src/main/java/com/mrfuzzihead/fluiddrawers/tile/TileTank.java` — **MODIFIED** (upgrade inventory stub)
- `src/main/java/com/mrfuzzihead/fluiddrawers/CommonProxy.java` — **MODIFIED** (GUI handler registration)
- `src/main/java/com/mrfuzzihead/fluiddrawers/FluidDrawers.java` — **MODIFIED** (added @Mod.Instance)

## Dependent classes
- `TileTank` (needs `getUpgradeInventory()` + `getDrawerGroup()`)
- `FluidDrawers` (needs `instance` field for GUI handler registration)
- `CommonProxy` (needs to call `FdGuis.init()`)

## API delta rules applied
- `L9GuiHandler`/`GuiIdentity` → vanilla `IGuiHandler` + `NetworkRegistry.registerGuiHandler`
- `L9GuiContainer` → vanilla `GuiContainer`
- `L9Container` → vanilla `Container`
- `GuiComponentFluidTank` → custom inline Tessellator fluid rendering
- `SlotItemHandler` → vanilla `Slot` backed by `IInventory`
- `I18n.func_135052_a` → `I18n.format`
- `field_147003_i` → `guiLeft`, `field_147009_r` → `guiTop`, etc.
- `GuiContainer.inventorySlots` is of type `Container` in 1.7.10 MCP (not `List`!)
- `Container.inventorySlots` is of type `List` (raw)

## Acceptance criteria
- [x] `IGuiHandler` registered via `NetworkRegistry.registerGuiHandler`.
- [x] `ContainerTank` (1.7.10 Container) with upgrade slots backed by stub IInventory.
- [x] `GuiTank` extends `GuiContainer`, draws `gui/tank.png` + custom fluid widget.
- [x] Dispatcher: empty-hand+sneak → open GUI (gated by `enableDrawerUI`). Plain right-click does nothing.
- [x] GUI-title lang key added (already existed in en_US.lang).
- [x] Build succeeds (spotless + compile + reobf).
- [ ] Launch + in-game test.
- [ ] Dedicated-server gate (runServer + join).

## Phase 8 DoD (from master-migration-spec.md)
- [x] `IGuiHandler` registered via `NetworkRegistry.registerGuiHandler`.
- [x] `ContainerTank` (1.7.10 Container) with upgrade slots backed by stub IInventory.
- [x] `GuiTank` extends `GuiContainer`, draws `gui/tank.png` + fluid widget.
- [x] Dispatcher: empty-hand+sneak → open GUI (gated by `enableDrawerUI`). Plain right-click does nothing.
- [x] GUI-title lang key added.
