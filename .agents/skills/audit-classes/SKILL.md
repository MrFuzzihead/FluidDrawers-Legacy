---
name: audit-classes
description: Audits classes for backporting a Minecraft mod from 1.12.2 to 1.7.10
---

# audit-classes

Enumerate every class in the target mod's source tree and tag each one by subsystem (block, item, tile entity, recipe, rendering, networking, worldgen, entity/AI, capability, cross-mod API). Do not modify or port any code in this workflow — output only a classification inventory. This inventory is what the master migration spec and per-file work orders get derived from, so completeness matters more than speed here: a missed class means a missing work order later.
If a class doesn't cleanly fit one subsystem tag (e.g. a TileEntity that also does rendering), tag it with all applicable subsystems and flag it as "tightly-coupled" so it can be considered as a cluster rather than split across separate work orders.

## Usage

Run this once at the start of a new backport, before any work orders exist. Re-run it if the mod's source tree changes significantly mid-project (e.g. after pulling a new upstream 1.12.2 version).

## Steps

1. Walk the full source tree (`src/main/java/...`) and list every `.java` class file with its package path. Exclude generated/build output and test directories.
2. For each class, inspect imports, superclass, and key method signatures to determine its subsystem tag(s) from the list above. Note any class that imports capability APIs, registry events, or JSON-model/blockstate machinery — these are the highest-signal indicators of subsystem and of 1.12.2-specific API surface that Section 3's delta rulebook will need to touch.
3. Output a single markdown table (class path | subsystem tag(s) | tightly-coupled cluster if any | one-line note on notable API usage) and save it as `docs/class-audit.md` in the mod repo. Do not proceed to writing work orders in this workflow — hand the table off for the master migration spec step.
