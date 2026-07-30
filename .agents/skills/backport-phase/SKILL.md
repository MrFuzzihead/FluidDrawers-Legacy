---
name: backport-phase
description: Execute one phase of the FluidDrawers 1.12.2 -> 1.7.10 backport against the granular phase list in docs/master-migration-spec.md.
---

# backport-phase

Execute one phase of the port order (as defined in `docs/master-migration-spec.md`'s phase list, e.g. Phase 0, Phase 1, ..., Phase 15) against the classes tagged for that phase in `docs/class-audit.md`. Apply the 1.12.2->1.7.10 API delta rulebook rules (`.clinerules/01-api-delta-1.12.2-1.7.10.md`) relevant to this phase's subsystem. Follow the standing rules at all times: never guess a mapped method/field name (emit `// TODO: verify against 1.7.10 javadoc` instead), and always produce a compiling stub before implementing real logic.

Only touch classes belonging to the requested phase and its declared dependencies (e.g. Phase 2 (renderer) may reference already-ported Phase 1 (block) classes, but should not pre-emptively port Phase 3 (TE) classes). If a class needed as a dependency hasn't been ported yet, stop and report it rather than porting it out of order.

## Usage

Invoke per phase, in port order, once the class audit and master migration spec exist. Example: `backport-phase phase 1`. Do not skip ahead to a later phase before the current one passes `verify-phase` — the port order exists specifically so compile errors cascade downward instead of sideways.

## Steps

1. Pull the list of classes tagged for the requested phase from `docs/class-audit.md`, and pull the relevant rows from the API delta rulebook (`.clinerules/01-api-delta-1.12.2-1.7.10.md`) for that phase's subsystem. Confirm all dependency classes from earlier phases are already ported; if not, halt and report the gap.
2. For each class in the phase, produce a compiling stub first (correct package, imports, class signature, method stubs with `// TODO` bodies where the mapped API is uncertain), then fill in real logic once the stub compiles clean.
3. Once every class in the phase compiles and stubs/logic are in place, write or update the per-file work order doc for each class under `docs/work-orders/` (source file, dependents, delta rules applied, acceptance criteria) and hand off to `verify-phase` before starting the next phase.


