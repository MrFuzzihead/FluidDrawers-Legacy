# Name
Master Migration Spec

# Instructions
Maintain a single living doc — `docs/master-migration-spec.md` — as the source of truth for this backport. It must contain: mod purpose/scope, source Forge build (1.12.2) and target Forge build (1.7.10) with their respective MCP mapping sets, the subsystem inventory (pulled from `docs/class-audit.md`), the phase order this mod will follow, and a definition-of-done per phase. Do not let phase-level detail (individual class work) leak into this doc — that belongs in per-file work orders. This doc stays high-level: it should stay readable in one sitting even as the project grows.

Treat this as append/update-only, never a full rewrite: when a phase completes, update its status and definition-of-done checkboxes rather than regenerating the doc. If the class audit changes (e.g. new classes found after an upstream pull), update the subsystem inventory section and flag which phases are affected.

# Usage
This is a standing rule, not a one-shot workflow — active for the life of this mod's backport. Cline should consult and update this doc automatically whenever a phase starts, completes, or the subsystem inventory changes, without needing to be invoked with a slash command. Keep this rule toggled on for this mod's workspace; toggle off only if working on an unrelated task in the same repo.

# Steps

**First** — On backport start, create `docs/master-migration-spec.md` and populate scope, Forge builds, and MCP mapping sets before any phase work begins.

**Second** — Pull the subsystem inventory from `docs/class-audit.md` once the audit workflow has run, and lock in the phase order (defaulting to Section 2's dependency-driven order unless this mod has unusual cross-subsystem dependencies that justify deviating).

**Third** — After each phase's `verify-phase.md` passes, update that phase's status and definition-of-done in this doc before `backport-phase.md` starts the next phase.
