# Getting Started with the AI Agent Harness

This guide helps you onboard as an AI agent in the Android Browser Helper repository.

## 1. Environment Setup

The harness is integrated into the workspace via the `.agents/` directory at the root.
*   **Active Rules**: When you start a task, always read [`AGENTS.md`](../AGENTS.md) first to load the project rules.
*   **Routing Table**: Use [`RULES.md`](RULES.md) to locate specific guidelines (Style, Testing, Dependencies).

## 2. Typical Workflows

### Workflow A: Implementing a Code Change
When asked to fix a bug or add a feature, follow the standard workflow in [`prompts/common.md`](prompts/common.md):
1.  **Audit**: Read the relevant source files and call sites first.
2.  **Edit**: Apply the change matching the style in [`rules/STYLE.md`](rules/STYLE.md).
3.  **Test**: Verify using JVM Robolectric tests or Instrumentation tests as described in [`rules/TESTING.md`](rules/TESTING.md).
4.  **Check**: Review your changes against [`rules/REVIEWS.md`](rules/REVIEWS.md) before presenting them.

### Workflow B: Writing Designs & Execution Plans
For larger tasks, use the `doc-writer` skill:
1.  **Invoke Doc Writer**: Use the `doc-writer` skill to start a GAN (Generator-Discriminator) loop to draft your document.
2.  **Templates**: The skill uses templates in [`templates/designs.md`](templates/designs.md) and [`templates/plans.md`](templates/plans.md).
3.  **Registration**:
    *   Save designs to [`designs/`](designs/) and register them in [`designs/index.md`](designs/index.md).
    *   Save plans to [`plans/`](plans/) and register them in [`plans/index.md`](plans/index.md).
