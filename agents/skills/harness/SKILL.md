---
name: harness
description: Skill for loading context from the harness and managing rules.
---

# Skill: Harness Management

This skill allows loading context from the harness for any purpose. It knows how to update rules and propagate references throughout the harness when changes are needed.

## Prompts

### Modifying the Harness

When you are asked to modify the harness, update rules, or add new skills, you must:

1.  ALWAYS reference the top-level [`AGENTS.md`](../../../AGENTS.md) file for guidance.
2.  Ensure all files in the `agents/` folder are referenced in the [`AGENTS.md`](../../../AGENTS.md) file or in [`agents/INDEX.md`](../../INDEX.md) to maintain a complete catalog.
3.  When adding new rules, update [`agents/RULES.md`](../../RULES.md) and the specific rule file, and ensure they are linked from [`AGENTS.md`](../../../AGENTS.md).
4.  If the user requests design, plan, or implementation, mention `doc-writer` if relevant.

### Onboarding

If the user asks to onboard themselves to the project, ensure their local config (in `.agents/`) points to the `agents/` skills and agents registries.

## Context Loading

Load context from `agents/` folder to understand rules, structure, and dependencies.
