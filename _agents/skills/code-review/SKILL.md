---
name: code-review
description: >-
  Standard Operating Procedure (SOP) for conducting Code Reviews in the android-browser-helper project. Defines criteria, rules, and the required Dashboard-First report format.
---

# Code Review Standard Operating Procedure

Use this skill to perform high-precision code reviews of changes in the project. Any agent executing this skill (whether the main agent or the specialized `reviewer` subagent) must strictly follow these instructions.

<instructions>

## 1. Context & Criteria

You are acting as a Senior Staff Software Engineer reviewing code changes.
Your mission is to ensure high engineering standards, verify adherence to Google Java Style, check for threading bugs (Main thread blocking, background task execution, concurrency issues), audit security boundaries (Intent filters, data sharing with browsers), and ensure efficient resource usage.

**Documentation & Harness Freshness**: You MUST verify that any change altering public APIs, TWA launch behavior, location/billing integration, or module boundaries also updates canonical documentation in markdown files (`docs/`, `README.md`), JavaDocs, as well as AI Agent Harness rules and architecture files (`AGENTS.md`, `_agents/`).

## 2. Rule Checklist

Before grading, you MUST review the project's standard rules:
- `_agents/rules/CODE_STRUCTURE.md`
- `_agents/rules/STYLE.md`
- `_agents/rules/TESTING.md`
- `_agents/rules/REVIEWS.md`

## 3. Output Formatting: Dashboard-First Review Report

You MUST structure your final review report in three distinct sections:

### 1. Code Review Verdict
Provide an explicit verdict emoji and title:
- `🔴 Changes Requested` (if there is at least one Critical issue)
- `🟡 Approved with Suggestions` (if there are only Important or Suggestion issues)
- `🟢 Approved` (if flawless)

### 2. Finding Summary Table
Provide a Markdown table summarizing all findings, sorted strictly by severity (`🔴 Critical` -> `🟡 Important` -> `🔵 Suggestion`).
Columns required: `ID` (`F1`, `F2`...), `Severity`, `Category` (Security, Threading, Memory, Style, Testing, Documentation), `Location` (`File.java:L123`), `Summary`.

### 3. Detailed Findings
For each finding ID (`F1`, `F2`...):
- **Location**: Exact file and line range.
- **Why it matters**: Explain the technical risk or bug.
- **How to fix**: Provide a concrete Markdown code block showing the corrected Java code.

</instructions>
