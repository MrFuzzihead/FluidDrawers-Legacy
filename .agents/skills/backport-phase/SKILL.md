---
name: backport-phase
description: Brief description of what this skill does
---

# backport-phase

Execute one phase of the port order (Scaffolding, Registration, Data backend, Recipes, Networking, Rendering, GUIs/Containers, Worldgen/Entities, Cross-mod API) against the classes tagged for that phase in `docs/class-audit.md`. Apply the 1.12.2→1.7.10 API delta rulebook rules relevant to this phase's subsystem. Follow the standing rules at all times: never guess a mapped method/field name (emit `// TODO: verify against 1.7.10 javadoc` instead), and always produce a compiling stub before implementing real logic.

Only touch classes belonging to the requested phase and its declared dependencies (e.g. Recipes phase may reference already-ported Registration-phase classes, but should not pre-emptively port Rendering-phase classes). If a class needed as a dependency hasn't been ported yet, stop and report it rather than porting it out of order.

## Usage

Invoke per phase, in port order, once the class audit and master migration spec exist. Example: `/backport-phase/SKILL.md registration`. Do not skip ahead to a later phase before the current one passes `verify-phase/SKILL.md` — the port order exists specifically so compile errors cascade downward instead of sideways.

## Steps

1. Pull the list of classes tagged for the requested phase from `docs/class-audit.md`, and pull the relevant rows from the API delta rulebook for that phase's subsystem (e.g. Registration phase → `RegistryEvent.Register<T>` → `GameRegistry.registerBlock/registerItem` row). Confirm all dependency classes from earlier phases are already ported; if not, halt and report the gap.
2. For each class in the phase, produce a compiling stub first (correct package, imports, class signature, method stubs with `// TODO` bodies where the mapped API is uncertain), then fill in real logic once the stub compiles clean.
3. Once every class in the phase compiles and stubs/logic are in place, write or update the per-file work order doc for each class (source file, dependents, delta rules applied, acceptance criteria) and hand off to `verify-phase.md` before starting the next phase.

