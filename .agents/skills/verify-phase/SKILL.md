---
name: verify-phase
description: Verifies the phase of work for the backport.
---

# verify-phase

Run the verification checklist for a phase that `backport-phase\SKILL.md` has just completed, before allowing progression to the next phase in the port order. This workflow does not write or fix code — it only checks and reports. If a check fails, report exactly which class/behavior failed and stop; do not attempt fixes inside this workflow (route back to `backport-phase\SKILL.md` or a manual fix instead).

## Usage

Invoke immediately after finishing a phase's work orders, before starting the next phase. Example: `/verify-phase/SKILL.md recipes`. Treat this as a gate: a failed verification blocks moving on, since later phases assume earlier ones are solid (e.g. Rendering assumes Registration is fully correct).

## Steps

1. Compile check: build the mod and confirm zero errors/warnings tied to this phase's classes. Flag any remaining `// TODO: verify against 1.7.10 javadoc` stubs still present as unresolved, not as failures — these are expected checkpoints, not bugs.
2. Dev client load check: launch the 1.7.10 dev client and confirm the mod loads without crashing, with no missing-registration or classloading errors traceable to this phase's classes.
3. Behavior check: run the phase-specific in-game checks from the verification checklist doc (e.g. for Recipes — craft each recipe added this phase and confirm correct output; for Networking — confirm packets sync TileEntity data across a save/reload). Record pass/fail per check in `docs/verification-log.md` and report the summary.

