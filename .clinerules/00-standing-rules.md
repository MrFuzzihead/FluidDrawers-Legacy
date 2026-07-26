## Standing Rules / Reusable Skills

- **Never guess a mapped method/field name.** If unsure, emit `// TODO: verify against 1.7.10 javadoc` rather than inventing a signature. Cheap rule, saves hours of hallucinated-API debugging.
- **Always produce a compiling stub first** (even with `// TODO` bodies) before implementing real logic, so cascading reference errors surface immediately instead of at the end.
- **Model role split** (fits limited VRAM budgets): planning/architecture model owns the master plan and per-file work orders; daily-coder model executes each work order in an agentic harness (e.g. Cline); planning model does periodic diff review rather than writing every line itself.
- **Keep the delta rulebook mod-agnostic** and versioned separately from mod-specific work orders — reuse it for any future 1.12.2→1.7.10 (or 1.8→1.7.10, 1.10.2→1.7.10, etc.) port.
