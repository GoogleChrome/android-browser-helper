---
name: doc-writer
description: >-
  GAN-based workflow for authoring Technical Design Documents and Execution Plans. Use when asked to "write a design", "author an execution plan", or execute GAN iterations for documentation in this project.
---

# Document Writer

## Hydration Parameters

The following parameters are used to configure the document writer. They can usually be inferred dynamically from the workspace structure:

- `projectName`: Name of the project (default: `android-browser-helper`)
- `documentName`: The name of the document being created (Design or Execution Plan)
- `agentRulesDoc`: Path to the project's `AGENTS.md` (default: `AGENTS.md`)
- `agentHarnessDirectory`: Path to the agent folder (default: `_agents/`)
- `designDirectory`: Directory where new designs are saved (default: `_agents/designs/`)
- `executionPlanDirectory`: Directory where new execution plans are saved (default: `_agents/plans/`)

Default Parameters:

- `designNamingConvention`: `0001-feature-design.md`
- `executionPlanNamingConvention`: `0001-feature-plan.md`
- `designTemplate`: `_agents/templates/designs.md`
- `executionPlanTemplate`: `_agents/templates/plans.md`

## Overview

This skill provides a Generator-Discriminator (GAN) workflow designed to help write Technical Design Documents (Designs) and Execution Plans (Plans) for the Android Browser Helper project. It orchestrates collaboration between specialized subagents to produce high-quality, reviewable artifacts.

---

## The Roles

You are the **Orchestrator**. Coordinate between two specialized subagents:

1.  **The Planner (Generator):** Generates initial drafts for the design or execution plan.
2.  **The Evaluator (Discriminator):** Reviews the drafts against project standards and rules.

---

## Orchestration Workflow

### Phase 1: Initialization

1.  **Identify The Flow**: Determine if the user wants to write a **Design Document** or an **Execution Plan**.
    - If **Design Document**: Use `designTemplate` (`_agents/templates/designs.md`).
    - If **Execution Plan**: Use `executionPlanTemplate` (`_agents/templates/plans.md`).
2.  **Dynamic Parameter Inference**: If the calling agent or user did not specify directories, use defaults.
3.  **Initialize State Tracking**: Create a Markdown file `_agents/gan_iteration_status.md` to track progress throughout the GAN loop.
4.  **Invoke Planner (Generator)**: Spawn a Planner subagent with `Model="heavy"` and `Workspace="inherit"` using the appropriate prompt template below.
5.  **Invoke Evaluator (Discriminator)**: Spawn an Evaluator subagent (e.g., using `adversarial_reviewer` persona if defined, or a self-configured evaluator) to critique the draft.

#### Planner Prompt Template (Design Document Flow)

```text
You are the Planner for the {{projectName}} project.

1.  ALWAYS read the project guidelines at `{{agentRulesDoc}}` before starting.
2.  Reference the Design Document template and instructions at `{{designTemplate}}` when creating the draft.
3.  Ensure your design strictly abides by project architecture, style, and dependency rules.
4.  Provide your complete markdown output in your response to the Orchestrator.

Your task is: [INSERT TASK DESCRIPTION HERE]
```

#### Planner Prompt Template (Execution Plan Flow)

```text
You are the Planner for the {{projectName}} project.

1.  ALWAYS read the project guidelines at `{{agentRulesDoc}}` before starting.
2.  Reference the Execution Plan template and instructions at `{{executionPlanTemplate}}` when creating the draft.
3.  ALWAYS ensure each milestone maps to a single PR/CL and includes concrete `./gradlew` build and test commands.
4.  Provide your complete markdown output in your response to the Orchestrator.

Your task is: [INSERT TASK DESCRIPTION HERE]
```

#### Evaluator Prompt Template

```text
You are the Skeptical Architect and Evaluator for the {{projectName}} project.

1.  ALWAYS read the project guidelines at `{{agentRulesDoc}}` before starting.
2.  Evaluate the planner's draft against the template instructions (`{{designTemplate}}` or `{{executionPlanTemplate}}`).
3.  Critique for hidden assumptions, security boundaries, and test coverage.
4.  If the work is flawless, explicitly state "LGTM".
```

---

### Phase 2: Reactive Message Loop

1.  **Planner Draft Received**: Update `gan_iteration_status.md` and forward the draft to the Evaluator via `send_message`.
2.  **Evaluator Critique Received**:
    - If the Evaluator replies "LGTM" or you reach the iteration limit of **3 rounds**, proceed to Phase 3.
    - Otherwise, forward the critique back to the Planner instructing them to apply adjustments.

---

### Phase 3: Human Approval

1.  Present the draft to the user for approval using visible text. Ask them to reply with `Accept` or `Comment and Iterate`.
2.  If **Accept**: Proceed to Phase 4.
3.  If **Comment and Iterate**: Forward feedback to the Planner, reset iteration count, and loop back.

---

### Phase 4: Finalization

1.  Write the approved document to the designated directory:
    - Designs → `{{designDirectory}}0001-feature-design.md`
    - Plans → `{{executionPlanDirectory}}0001-feature-plan.md`
2.  Register the new document in the corresponding index file (`index.md`).
3.  Notify the user that the document has been successfully saved.
