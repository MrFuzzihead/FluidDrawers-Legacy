---
name: verify-phase
description: Run the verification gate for a completed phase of the FluidDrawers 1.12.2 -> 1.7.10 backport.
---

# verify-phase

Run the verification checklist for a phase that `backport-phase` has just completed, before allowing progression to the next phase in the port order (as defined in `docs/master-migration-spec.md`). This workflow does not write or fix code — it only checks and reports. If a check fails, report exactly which class/behavior failed and stop; do not attempt fixes inside this workflow (route back to `backport-phase` or a manual fix instead).

## Usage

Invoke immediately after finishing a phase's work orders, before starting the next phase. Example: `verify-phase phase 1`. Treat this as a gate: a failed verification blocks moving on, since later phases assume earlier ones are solid (e.g. Phase 7 (rendering) assumes Phase 1 (block registration) is fully correct).

## Steps

1. **Compile check:** `./gradlew build` and confirm zero errors/warnings tied to this phase's classes. Flag any remaining `// TODO: verify against 1.7.10 javadoc` stubs still present as unresolved, not as failures — these are expected checkpoints, not bugs.
2. **Dev client load check:** `./gradlew runClient` and confirm the mod loads without crashing, with no missing-registration or classloading errors traceable to this phase's classes.
3. **Dedicated-server gate (phases touching sync, proxy split, or GUI — see section 4 of the plan):** `./gradlew runServer` and join from a client (`./gradlew runClient` pointed at the server). Confirm no proxy-split crash, no client-only-class error when opening a GUI, and no packet-side bug.
4. **Behavior check:** Run the phase-specific in-game checks from the plan's phase description in `docs/backport-plan.md`. Record pass/fail per check in `docs/verification-log.md` and report the summary.


