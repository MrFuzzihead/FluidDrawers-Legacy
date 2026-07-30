# Fluid Drawers — Legacy (1.7.10)

[![Minecraft 1.7.10](https://img.shields.io/badge/Minecraft-1.7.10-blue)](https://minecraft.net/)
[![Forge 10.13.4.1614](https://img.shields.io/badge/Forge-10.13.4.1614-blue)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A backport of **FluidDrawers** (by MrFuzzihead) from Minecraft 1.12.2 to 1.7.10.  
Adds a **Fluid Tank** block that stores, displays, and interacts with fluids via right-click with buckets/containers, a GUI, and integration with **Storage Drawers** 1.7.10.

> **Status:** Production-ready (v1.0.x). Core tank, upgrades, seal/security, WAILA, and SD Controller integration are complete and tested. The framed/custom tank variant is deferred.

---

## Features

- **Fluid Tank** — stores up to 16 buckets (default, upgradable) of any Forge fluid in a single block.
- **See-through glass frame** — the fluid level is visible inside the tank (TESR-rendered, respects world lighting).
- **GUI** — view fluid type/amount, manage upgrades (7 upgrade slots).
- **Bucket & container interaction** — right-click with a bucket (or any `IFluidHandler` container) to fill/drain. Works with any mod's fluid containers.
- **Tape seal** — right-click with packing tape (from Storage Drawers) to seal a tank, preserving its contents when broken. Placing a sealed tank restores everything (fluid, upgrades, attributes, owner).
- **Upgrades** — supports Storage Drawers 1.7.10 upgrades:
  - **Storage upgrades** (iron/gold/obsidian/diamond/emerald/ruby/tanzanite) — increases capacity (configurable multiplier).
  - **Void upgrade** — excess fluid is voided when the tank is full.
  - **Creative vending upgrade** — infinite fluid source (renders a distinct vending texture).
  - **Lock upgrade** — prevents the fluid type from changing.
  - **Concealment key** — hides the fluid sprite (useful for builds where the fluid shouldn't show).
  - **Quantify key** — toggles a floating "fluid name / X mB" label on the tank's four sides.
  - **Personal key** — secures the tank to a player; only the owner can interact.
  - **Redstone upgrade** — emits redstone signal proportional to fill level (weak on all sides, strong on top).

- **Packing tape** (from Storage Drawers) — seal a tank to pick it up while preserving fluid, upgrades, owner, and attributes.
- **Drawer Controller integration** — tanks on the same Storage Drawers Controller network are accessible as fluid endpoints. Right-click the controller with a bucket to fill/drain connected tanks. Pipes/hoppers can pump to/from the controller, which distributes fluid across all discovered tanks.
- **WAILA support** — displays fluid type, amount, capacity, and locked/void/sealed/protected/creative status.
- **Item tooltip** — sealed tanks in the inventory show fluid type, amount, and attribute status.
- **Custom item renderer** — sealed tank items display the fluid + overlays + tape (matching the in-world appearance), so you can see what's inside at a glance.

## Dependencies

| Dependency                                                                         | Required | Notes                                                                                                                                                            |
|------------------------------------------------------------------------------------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| [**Storage Drawers 1.7.10**](https://github.com/MrFuzzihead/StorageDrawers-Legacy) | **Hard** | Provides the upgrade system, packing tape, lock/concealment/quantify keys, creative vending upgrade, security (personal key), and the Drawer Controller network. |
| [**WAILA**](https://www.curseforge.com/minecraft/mc-mods/waila)                    | Optional | Shows tank fluid info in the WAILA HUD overlay.                                                                                                                  |
| [**UniMixins**](https://github.com/LegacyModdingMC/UniMixins)                      | **Hard** | Required for the Drawer Controller mixin (controller → IFluidHandler). Bundled Jar-in-Jar via the build system.                                                  |

---

## Installation

1. Install Minecraft Forge **10.13.4.1614** for Minecraft 1.7.10.
2. Copy the `FluidDrawers-Legacy` JAR into your `mods/` folder.
3. Also install **Storage Drawers 1.7.10** (the fork linked above) and **UniMixins**.

> **Important:** The 1.7.10 Storage Drawers fork must be the version that ships the upgrades and API classes this mod depends on. The standard "v1.10.25" from GTNH may work but the linked fork is the recommended build.

---

## Usage

### Basic tank

```
[Iron Ingot] [Glass Pane] [Iron Ingot]
[Glass Pane] [Iron Ingot] [Glass Pane]
[Iron Ingot] [Glass Pane] [Iron Ingot]
```

1. Craft the **Fluid Tank** using the recipe above.
2. Place it in the world. The see-through glass frame shows the tank interior.
3. Right-click with a **fluid container** (water bucket, lava bucket, any Forge-fluid-filled item) to fill.
4. Right-click with an **empty container** to drain.
5. Right-click with the **tank item** (or open via GUI) to inspect contents and manage upgrades.

### GUI

Open the tank to see:
- **Fluid gauge** — shows the stored fluid level and type (with tinted sprite).
- **Upgrade slots** (7 slots) — insert Storage Drawers upgrades.
- **Player inventory** — standard survival inventory below.

### Sealing (packing tape)

1. Right-click a placed tank with **packing tape** (from Storage Drawers) to seal it.
2. Break the sealed tank — it drops as an item carrying all its data (fluid, upgrades, owner, attributes).
3. Place the item to restore the tank+contents+upgrades as they were.
4. A sealed tank placed in-world shows the tape overlay + fluid + overlays (contents are visible through the tape).

### Security (personal key)

1. Right-click a tank with a **personal key** while sneaking to set the owner.
2. Only the owner can open the GUI, fill/drain, remove upgrades, toggle lock/conceal/quantify, or re-secure.
3. Right-click with a personal key (not sneaking) to clear the owner.

### Redstone output

When a **redstone upgrade** is inserted, the tank emits:
- **Weak power** on all six sides — power level = `clamp(1 + 14 × fill / capacity, 1, 15)`.
- **Strong power** on the bottom-face-up side (UP).
  *Design note:* Matches the 1.12.2 FD — energizes adjacent dust, hoppers, and machines on every side, not just the bottom.

---

## Configuration

`fluiddrawers.cfg` is generated on first launch in the `config/` directory:

| Property                   | Default                    | Description                                                                        |
|----------------------------|----------------------------|------------------------------------------------------------------------------------|
| `baseTankCapacity`         | 16000                      | Base capacity in mB (16 buckets)                                                   |
| `storageUpgradeMultiplier` | 4.0, 6.0, 12.0, 16.0, 24.0 | Multiplied by base capacity per upgrade level (iron/gold/obsidian/diamond/emerald) |
| `enableDismantling`        | true                       | Allow pick-block (creative) to unseal tanks                                        |
| `enableRedstoneUpgrades`   | true                       | Allow redstone-comparator-style power output                                       |
| `quantifyShowsFluidName`   | true                       | Show the fluid name alongside the amount in the quantify-key label                 |
| `wailaEnabled`             | true                       | Enable WAILA integration                                                           |

---

## Drawer Controller Integration

Tanks placed within range of a Storage Drawers **Drawer Controller** are automatically discovered by the controller's network (via `INetworked`). The controller implements `IFluidHandler`, so:

- Right-click the controller with a bucket → fills/drains from connected tanks.
- Pipes/hoppers (Thermal Expansion, BuildCraft, etc.) can pump to/from the controller → fluid is distributed to tanks (fill) or drained from tanks (drain).
- The controller discovers tanks by BFS from its own `INetworked` graph (no reflection — the mixin re-scans rather than peering into private controller internals).

---

## Building from source

```bash
git clone https://github.com/MrFuzzihead/FluidDrawers-Legacy.git
cd FluidDrawers-Legacy
./gradlew build
```

The compiled JARs are in `build/libs/`:
- `FluidDrawers-<version>.jar` — development (unobfuscated)
- `FluidDrawers-<version>-reobf.jar` — production (reobfuscated, for distribution)

Requires JDK 17+ (build system uses Jabel/JVM Downgrader, produces Java 8 bytecode).

---

## Project structure

| Package                                        | Contents                                                                                            |
|------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| `com.mrfuzzihead.fluiddrawers`                 | Main mod class, proxies, config, creative tab                                                       |
| `com.mrfuzzihead.fluiddrawers.block`           | `BlockTank`                                                                                         |
| `com.mrfuzzihead.fluiddrawers.tile`            | `TileTank` (tile entity with fluid storage, upgrade inventory, attribute/security management)       |
| `com.mrfuzzihead.fluiddrawers.drawers`         | Fluid drawer & group API (`FluidDrawer`, `FluidDrawerGroup`, `SimpleFluidDrawer`, etc.)             |
| `com.mrfuzzihead.fluiddrawers.client.renderer` | `BlockTankRenderer` (ISBRH for the hollow-frame block model), `ItemRendererTank` (sealed-tank item) |
| `com.mrfuzzihead.fluiddrawers.client.tesr`     | `RenderTileTank` (fluid-level TESR + quantity label)                                                |
| `com.mrfuzzihead.fluiddrawers.client.gui`      | `GuiTank` (GUI with fluid gauge + upgrade slots)                                                    |
| `com.mrfuzzihead.fluiddrawers.inventory`       | `ContainerTank`, `SlotDrawerUpgrade`                                                                |
| `com.mrfuzzihead.fluiddrawers.init`            | `ModBlocks`, `ModRecipes`, `FdGuis`                                                                 |
| `com.mrfuzzihead.fluiddrawers.integration`     | `WailaIntegration`                                                                                  |
| `com.mrfuzzihead.fluiddrawers.mixins`          | `MixinTileEntityController` (Drawer Controller IFluidHandler) + mixin loader                        |
| `com.mrfuzzihead.fluiddrawers.util`            | Fluid handler adapters and interaction utilities                                                    |

---

## Credits

- **MrFuzzihead** — FluidDrawers 1.7.10 backport
- **phantamanta44** - original FluidDrawers 1.12.2 mod
- **jaquadro** — Storage Drawers
- **GTNewHorizons** — RetroFuturaGradle build toolchain
- **LegacyModdingMC** — UniMixins

## License

This project is licensed under the **MIT License**. See [LICENSE](LICENSE) for details.  
The original FluidDrawers 1.12.2 source is by MrFuzzihead, used under the MIT license.
