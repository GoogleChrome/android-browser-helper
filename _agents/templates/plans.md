# Execution Plans (ExecPlans)

**Agent Preamble:**

> **CRITICAL:** Before executing any milestone, you MUST read the project's `AGENTS.md` to understand operating procedures. Run tests and verify code quality before completing each milestone. Proceed to next milestone only after user confirmation.

This document describes the requirements for authoring an Execution Plan (ExecPlan) for the Android Browser Helper project.

## Purpose of an Execution Plan

An Execution Plan is a **living, persistent markdown artifact** that tracks multi-day, multi-PR/CL projects. While a Design Document defines *what* to build and *why*, an Execution Plan defines *how* to execute the implementation step-by-step across reviewable Pull Requests (PRs) or Changelists (CLs).

**Key Principles:**

1.  **Milestone = PR/CL Boundary:** Each milestone MUST correspond to exactly ONE PR/CL. Keep changes small and focused.
2.  **Observable Verification:** Every milestone MUST list explicit build and test commands (using `./gradlew`) with expected outcomes.
3.  **Idempotence:** Steps should be safe to re-run without breaking workspace state.
4.  **Living State Tracking:** Keep the check-boxes up to date as milestones complete. Document surprises, bugs, and architectural decisions discovered along the way.

## Authoring an Execution Plan

### File Naming and Location

All execution plans must be placed in the `_agents/plans/` subdirectory. They must be explicitly numbered and follow the naming convention: `0001-feature-plan.md`.

**Registration Requirement:** Before creating a new plan, you **must** register it in the index file (`_agents/plans/index.md`) and link it back to its parent Design Document.

### Required Structure

```markdown
---
id: "0001"
title: "Plan: [Feature Name]"
project: "android-browser-helper"
author: "user@google.com"
status: "in-progress"
date: "2026-08-12"
design_doc: "../designs/0001-feature-design.md"
bug: "b/123456789" # or GitHub Issue link
---

<!--
**Agent Preamble:**
> **CRITICAL:** Run tests and verify code quality before completing each milestone.
-->

## 1. Purpose / Big Picture

Explain in a few sentences what the user gains after this change and how they can see it working. State the user-visible behavior enabled.

## 2. Context and Orientation

Describe current state relevant to this task. Reference key files by relative path. Define any non-obvious terms.

## 3. Progress

- [ ] **Milestone 1: [Name]**
- [ ] **Milestone 2: [Name]**

## 4. Surprises & Discoveries

Document unexpected behaviors, optimizations, or bugs discovered during implementation.

## 5. Decision Log

Record decisions made while working on the plan, including rationale and date.

## 6. Plan of Work (Milestones)

### Milestone 1: [Milestone Name]

*   **Concrete Steps:**
    - Edit files: `androidbrowserhelper/src/main/java/com/google/androidbrowserhelper/trusted/SomeFile.java`
    - Build command: `./gradlew :androidbrowserhelper:assembleDebug`
    - Test command: `./gradlew :androidbrowserhelper:test` (or `connectedAndroidTest` if needed)
*   **Interfaces and Dependencies:**
    - [List classes or methods created or modified]
    - [Check dependency constraints]
*   **Validation and Acceptance:**
    - [State what to observe to confirm the milestone is successful]
    - [Verify code formatting]
    - [Verify documentation and harness freshness: ensure canonical documentation in markdown files (docs/, README.md), JavaDoc, as well as AI Agent Harness files is updated]
*   **Idempotence and Recovery:**
    - [State if steps can be safely re-run]
```
