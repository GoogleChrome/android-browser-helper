---
name: review
description: >-
  Spawns a specialized adversarial-reviewer subagent to perform a skeptical, rigorous
  Adversarial Design Review of a technical design document, proposal, or code diff.
---

# Adversarial Design Review Skill

Use this skill to perform high-precision, skeptical evaluations of technical proposals, design documents, or code diffs by delegating to the `adversarial_reviewer` subagent.

<instructions>

1.  **Identify Target**: Determine the path to the design file, document, or workspace diff to be reviewed.
2.  **Launch Subagent**: Invoke the specialized `adversarial_reviewer` subagent using the `invoke_subagent` tool.
    *   Specify `TypeName: "adversarial_reviewer"`
    *   Specify `Role: "Skeptical Architect"`
    *   Provide a clear objective referencing the target file's path, prompting the subagent to perform the design review.
3.  **Collect and Format Report**: Once the subagent returns its findings, present the finalized report directly to the user.

</instructions>

## Resources

This skill references the following agent definition:

*   [`_agents/agents/adversarial_reviewer/agent.json`](../../agents/adversarial_reviewer/agent.json)
