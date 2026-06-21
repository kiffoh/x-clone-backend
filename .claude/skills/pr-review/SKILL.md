---
name: pr-review
description: Review local branch changes against project conventions before opening an MR. Use when reviewing a diff, checking a feature branch, or before pushing. Covers JPA/JPQL correctness, GraphQL schema conventions, null-safety, and test standards.
allowed-tools: Read, Glob, Grep, Bash(git diff:*), Bash(git merge-base:*), Bash(git branch:*), Bash(git log:*), Bash(git status:*)
---

# PR Review

Review the local diff for this branch against a base branch and report findings.
This is **review-only** — surface issues, do not edit code unless explicitly asked.

Argument `$1` is the base branch to diff against (default: the repo's default branch).

## 1. Compute the diff

- Determine the base: use `$1` if provided, else detect `main` or `master`
  (`git branch --format='%(refname:short)'`).
- Find the merge base so you review only this branch's commits, not unrelated
  drift on the base: `git merge-base HEAD <base>`.
- Get the diff: `git diff <merge-base>...HEAD`. Start with `--stat` to see scope,
  then read the full diff (or per-file) for everything that changed.
- If there are uncommitted changes worth including, note them via `git status`
  and `git diff` / `git diff --cached`, and say which you reviewed.

## 2. Load the conventions

Read `conventions.md` in this skill directory before reviewing. It holds the
project's established patterns and the recurring issues that have bitten past
reviews. Check every changed file against it.

## 3. Review and report

For each changed file, check three things:
- **Correctness** — logic errors, null-safety, off-by-one, resource leaks, FK
  and constraint violations, concurrency.
- **Conventions** — every applicable item from `conventions.md`.
- **Tests** — is new or changed behavior covered, and will the tests actually run?

Group findings as **Blocking** / **Should-fix** / **Nit**. For each finding give:
`file:line`, what's wrong, why it matters, and a suggested fix. Cite the relevant
convention by name when one applies. If a category is clean, say so — do not
manufacture findings to fill it out.

## 4. Scale the review to the diff size

**Small diffs (≤5 files, single domain area):** review directly in this session.
No subagents — the overhead isn't worth it.

**Large or cross-cutting diffs (many files, multiple domain areas):** use a
Plan → Explore pipeline:

1. **Plan** — spawn a Plan subagent with the `--stat` output and the contents of
   `conventions.md`. Its job is to produce a review strategy: which areas changed,
   which are highest risk, which conventions apply to each area, and a suggested
   review order. It is planning the *review approach*, not planning code changes.
2. **Explore** — based on the plan, spawn one or more Explore subagents with
   focused briefs (e.g. "review the notification trigger changes against the
   null-safety and self-notification conventions"). Each scans its area in isolated
   context and reports findings back.
3. **Synthesize** — in this session, merge the Explore findings into the grouped
   report from step 3. Deduplicate, resolve any conflicts between agents, and
   present the final review.