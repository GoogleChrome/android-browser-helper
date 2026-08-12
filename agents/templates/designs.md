# Technical Design Documents (Designs)

**Agent Preamble:**

> **CRITICAL:** Before reading this document or authoring a design, you MUST read the project's `AGENTS.md` file to understand the architecture, style, and boundaries. Also review the style guide at `agents/rules/STYLE.md` and general coding workflow at `agents/prompts/common.md`.

This document describes the requirements for authoring a Technical Design Document for the Android Browser Helper project.

## Purpose of a Design

A Design document is strictly about **architecture**. Its purpose is to explore, define, and document the structure, data models, API surfaces, dependencies, and technical trade-offs of a proposed system *before* any implementation planning or coding begins.

A Design document answers the questions:

- "What problem are we solving?"
- "What is the shape of the data, Java classes, and Intent interfaces?"
- "What were the alternative approaches, and why were they rejected?"

**A Design Document is NOT an Execution Plan.** It does not contain implementation steps, file editing instructions, or milestones. It focuses entirely on establishing agreement on the technical architecture.

## Authoring a Design

When asked to author or propose a technical design, conduct thorough research of the existing repository context and generate a comprehensive markdown document. Ensure your design is objective and addresses the constraints of the system.

**Tailoring for Scale:** For smaller changes or refactors, many sections (e.g., Privacy, UI) may not apply. In such cases, do not delete the sections; instead, keep the headings and explicitly mark them as **N/A** (Not Applicable) with a brief, one-sentence explanation.

### File Naming and Location

All new design documents must be placed in the `agents/designs/` subdirectory. They must be explicitly numbered, use lowercase letters, and use hyphens for separation. They must follow the naming convention: `0001-feature-design.md`.

**Registration Requirement:** Before creating a new design document, you **must** register it in the index file (`agents/designs/index.md`) to prevent ID conflicts.

### Required Structure

Your Design document must follow this format:

```markdown
---
id: "0001"
title: "Design: [Feature Name]"
project: "android-browser-helper"
author: "user@google.com"
status: "draft"
date: "2026-08-12"
bug: "b/123456789" # or GitHub Issue link
---

<!--
**Agent Preamble:**
> **CRITICAL:** Before reading this design or writing any code, you MUST read the project's AGENTS.md.

**Execution Plans:**
*   [0001-example-plan](../plans/0001-example-plan.md) (Optional: Link to plans once created)
-->

## 1. Context and Goals

**Problem formulation:** What is the specific problem you are trying to solve? Describe the current state and its deficiencies.

**Background:** Discuss motivation, link to screenshots, related features, etc.

**Goals:** What are the objective requirements for a successful design?
*   Goal 1

**Non-Goals:** Explicitly state what this design will *not* attempt to solve.
*   Non-goal 1

## 2. Proposed Architecture

High-level architecture overview. How does this align with the project boundaries (e.g., Core Library vs. Location/Billing extensions)?

### Subsystems Affected
*   [ ] Core Library (`androidbrowserhelper`)
*   [ ] Location Delegation (`locationdelegation`)
*   [ ] Play Billing (`playbilling`)
*   [ ] Demos

### Thread Model
*   Does it perform work on the Main (UI) thread? If so, how do we avoid blocking it (e.g., using AsyncTask, Executors, or Coroutines if applicable)?
*   Does it interact with background services?

### Data Models & Schemas
Detail the shape of data. SharedPreferences structure, Bundle extras, or core domain classes.

### API Surface & Public Interfaces
Define public Java classes, methods, Intent Actions, and Extra keys.

## 3. Alternatives Considered
Explore at least one viable alternative. Describe approach and state trade-offs.

## 4. Core Principle Considerations

### Speed & Efficiency
*   **Main Thread Impact:** Does this add work to the Android Main thread?
*   **Startup & Critical Paths:** Is it on the critical path of TWA launch?
*   **APK Size Impact:** Expected impact on the library size.

### Security
*   **Intent Security:** Are intents properly secured? Are we exposing components to unauthorized apps?
*   **Data Sharing:** Are we safely sharing data with the browser provider?

### Stability & Simplicity
*   How do we handle cases where the preferred browser is not installed or doesn't support the required features?

## 5. Privacy & Accessibility

### Privacy
*   Does it collect/transmit user data?
*   What data is shared with the browser (URLs, location, billing info)?
*   Does it respect the user's choices/permissions?

### Accessibility (A11y)
*   If introducing UI components (e.g., splash screens, permission dialogs), do they support TalkBack and standard accessibility features?

## 6. Testing Plan
*   Unit tests (Robolectric).
*   Instrumentation tests (AndroidX Test).
*   Manual testing steps (if physical device interaction is required).

## 7. Detailed Implementation
Detailed architectural breakdown. Enumerate primary classes modified or created.

## 8. Future Work & Technical Debt
*   Deferred work or known limitations.
```

## Review and Approval

Once the Design is authored, it must be reviewed and approved by the user. Only after architectural consensus is reached via the Design document should an Execution Plan (see `agents/templates/plans.md`) be authored.
