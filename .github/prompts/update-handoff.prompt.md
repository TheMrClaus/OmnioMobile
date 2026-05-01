---
name: "Update Next Handoff"
description: "Refresh NEXT_AGENT_HANDOFF.md after a completed migration, bugfix, or validation slice. Use when recent changes and verification results need to be captured for the next agent."
argument-hint: "Describe what landed, what was deferred, and which validations ran"
agent: "agent"
---

Update [NEXT_AGENT_HANDOFF.md](../../NEXT_AGENT_HANDOFF.md) for the latest completed slice.

Requirements:

- Read [AGENTS.md](../../AGENTS.md) and the current [NEXT_AGENT_HANDOFF.md](../../NEXT_AGENT_HANDOFF.md) first.
- Use the prompt argument as the slice summary and prioritize that area when reviewing diffs, touched files, and validation commands.
- Rewrite only [NEXT_AGENT_HANDOFF.md](../../NEXT_AGENT_HANDOFF.md) unless another chat customization file clearly needs a matching update.
- Preserve the existing section structure:
  - `Baseline`
  - `What Landed`
  - `Validation`
  - `Important Scope Decisions`
  - `Recommended Next Slice`
  - `Guardrails`
  - `Nearby Files`
- Replace stale bullets instead of appending duplicate history.
- Record validation commands exactly as they were run and whether they passed.
- Call out unresolved risks, deferred work, and the next smallest practical slice.
- Keep bullets concrete and repo-specific; do not pad the handoff with generic summaries.
- If no validation was run, say that explicitly rather than guessing.