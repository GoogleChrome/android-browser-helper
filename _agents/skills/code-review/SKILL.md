---
name: code-review
description: >-
  Spawns a specialized reviewer subagent to perform a rigorous Senior Staff SWE Code Review on a diff, branch, or file, producing a Dashboard-First review report.
---

# Code Review Skill

Use this skill to perform high-precision code reviews of changes in the project by delegating to the `reviewer` subagent.

<instructions>

1.  **Identify Target**: Determine the file, branch diff, or commit to be reviewed.
2.  **Launch Subagent**: Invoke the specialized `reviewer` subagent using the `invoke_subagent` tool.
    *   Specify `TypeName: "reviewer"`
    *   Specify `Role: "Senior Staff Reviewer"`
    *   Provide a clear prompt instructing it to review the target diff or file against [`_agents/RULES.md`](../../RULES.md) and project coding standards.
3.  **Collect and Format Report**: Once the subagent returns its Dashboard-First report, present it directly to the user.

</instructions>

## Resources

This skill references the following agent definition:

*   [`_agents/agents/reviewer/agent.json`](../../agents/reviewer/agent.json)
