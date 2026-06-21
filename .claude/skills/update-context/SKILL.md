---
name: update-context
description: Update the project context document at the end of a working session. Run this after a PR review, feature slice, or any session that moved the project forward. Must run in the main session (not forked) because it needs conversation history to know what changed.
allowed-tools: Read, Write, Glob, Grep, Bash(find:*)
---

# Update Context

Update the project's context documentation based on what was accomplished and
discussed in this session.

## 1. Locate the context files

Search for CLAUDE.md files and any skill markdown files that contain project
context (architecture decisions, completed work, next steps). Use `find` and
`Glob` — don't assume paths.

## 2. Read current state

Read every context file found. Understand the existing structure before changing
anything.

## 3. Update each section

For each context file that needs changes based on this session:

- **Completed work**: add anything that was finished this session.
- **Next Steps**: remove items that are now done; add new items that were
  discussed or identified (e.g. from PR review findings, design discussions,
  or discovered issues).
- **Architecture Decisions Made**: add any new decisions, with the reasoning
  that led to them.

Only update sections where this session produced something relevant. Don't
rewrite sections that haven't changed.

## 4. Output

Output the full updated document(s) in markdown. Preserve the existing structure
and voice — these are living documents, not regenerated from scratch.

## Reminder

The user is learning Spring Boot. Guide with questions and concept pointers
rather than direct answers, unless the content is purely factual, syntactic, or
configuration-related. This applies to the context doc's framing of next steps
and open questions — phrase them as learning prompts, not instructions.