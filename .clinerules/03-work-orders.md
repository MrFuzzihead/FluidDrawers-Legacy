# Name
Per-File Work Orders

# Instructions
For every class or tightly-coupled cluster flagged in `docs/class-audit.md`, maintain a short work order doc under `docs/work-orders/<class-or-cluster-name>.md` containing exactly four things: source file path(s), dependent classes (what breaks if this changes), which API delta rulebook rows apply, and acceptance criteria (what "done" looks like for this specific class — usually tied to the phase's definition-of-done). Keep each one short enough to fit in context on its own; if a work order is growing long, it's a sign the cluster should be split or the class genuinely belongs to two phases.

These are the actual unit of work fed to the daily-coder model turn by turn — do not batch multiple unrelated classes into one work order, and do not let a work order reference classes outside its own phase's dependency set.

# Usage
Standing rule, active for the life of this mod's backport, same as the master migration spec rule. Cline should create or update a work order automatically as part of `backport-phase.md`, not on separate manual invocation. Toggle on alongside the master-migration-spec rule for this mod's workspace.

# Steps

**First** — Before implementing a class in `backport-phase.md`, check whether `docs/work-orders/<name>.md` already exists; if not, create it from the class-audit entry (subsystem tag, tightly-coupled cluster flag) and the relevant API delta rulebook rows.

**Second** — As the class is stubbed and implemented, keep the work order's acceptance criteria current — this is what `verify-phase.md` checks against, so it must describe observable behavior (compiles, loads, specific in-game result), not just "port this class."

**Third** — Once `verify-phase.md` confirms the class passes, mark the work order as complete rather than deleting it — completed work orders are what future backports (1.8→1.7.10, 1.10.2→1.7.10) can be diffed against to see what changed.
