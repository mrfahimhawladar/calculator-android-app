# AI Review and Feedback Loop

## Purpose

Use independent review and automated checks to catch defects before calculator changes
are merged. The target is a bounded feedback loop, not an endlessly running or
self-merging AI process.

## Required change flow

1. Make each code change on a topic branch and open or update a pull request to `main`.
   Do not make normal application-code changes directly to `main`.
2. The existing `Calculator Source CI` workflow runs on pull requests targeting
   `main` and pushes to `main`, and daily at 19:00 Bangladesh time (13:00 UTC). It
   performs JavaScript/shell syntax checks, regression tests, and the web build.
3. The repository owner has installed/authorized the CodeRabbit GitHub App. CodeRabbit
   uses `.coderabbit.yaml` and `AGENTS.md` to review pull requests. However, an App
   comment is not necessarily a completed review: the setup PR #1 produced a “Review
   skipped” message. Before treating AI review as validated, record the PR's current
   head commit SHA and require the completed review submission's commit SHA (or each
   actionable inline finding's commit SHA) to match that exact head SHA. An older review
   or comment attached to a previous commit does not validate newer changes; re-check
   after every new push.
4. At each Calculator automation run, inspect open pull requests, check statuses,
   CodeRabbit review comments, and unresolved threads. Record the current PR head SHA;
   count review evidence only when its commit SHA matches that head. Verify each finding
   against the current code, tests, and requirements before changing anything.
5. Fix valid defects in the same pull request, add regression tests, and wait for checks
   and review feedback to update. Re-review at most two correction passes per run; if
   significant issues remain, report them with links and stop for owner input.
6. Never auto-merge, suppress a failing check, fabricate a test result, or apply a review
   suggestion that weakens parser safety, offline-first behavior, or secret handling.
7. If CodeRabbit skips or fails to review a pull request, inspect its status/comment and
   settings. Try a manual `@coderabbitai review` request on the open PR. If it still
   skips, report the exact reason and continue deterministic CI checks; do not claim an
   AI review occurred.

## Parallel work and review polling protocol

Use this workflow for every task that requires a CodeRabbit review:

1. Start the requested change, run available checks, and make sure the PR head SHA is recorded.
2. Do not sit idle while CodeRabbit is processing. Work on a bounded, relevant parallel item such as inspecting adjacent code for edge cases, preparing a regression test, reviewing build/toolchain blockers, or improving non-conflicting documentation. Keep the pending-review change isolated: do not push extra commits to the same PR branch while a review is running unless necessary, because every new head can make the in-progress or completed review stale.
3. Check review status at meaningful checkpoints: after the parallel work item, after CI status changes, and immediately before reporting. Re-fetch the latest PR head SHA, review submissions, review comments, unresolved threads, and Actions status each time. Never count a review from an older SHA as current-head evidence.
4. Prefer an event-triggered GitHub workflow for immediate notification/triage when one is configured. Otherwise, perform bounded checks during the active run; do not busy-poll indefinitely or claim to run a background timer after the turn ends. If the review is still pending, report that explicitly and state the exact last-checked head SHA and status.
5. When a current-head review arrives, analyze each finding against source, project rules, and tests. Fix only valid issues on the same PR branch, add regression tests when practical, run CI, and request/rely on a fresh review of the resulting head. Continue useful parallel work while the next review is pending.
6. Limit the cycle to at most two correction passes per active run. Never auto-merge. If a third pass would be needed, the current head keeps failing, or required credentials/permissions are missing, stop and report the blocker with links and exact evidence.

## Resilient task execution and file safety

Apply these safeguards to all project changes and review follow-ups:

1. **Chunk work to fit tool limits.** Split large tasks into small independently verifiable stages. Read only the relevant file ranges, keep diffs focused, filter status output, and avoid oversized single calls or archive operations. Do not run concurrent writes to the same file.
2. **Checkpoint before changing state.** Read the canonical file and current revision first. Preserve a known-good backup or create a versioned draft before replacing files. Prefer a temporary file plus verification before atomic replacement. Never destroy the only verified copy before the replacement passes checks.
3. **Verify each operation.** After a file update, re-read the exact path/branch and inspect the diff. After ZIP creation, check archive integrity and contents. After Library upload, re-list/search the destination and confirm the exact path and size. A successful request alone does not prove the intended result is saved.
4. **Handle errors without repeated blind retries.** Classify the failure (rate limit, timeout/session loss, invalid arguments, stale version, permissions, network/DNS, test/build failure). Preserve the last good state, inspect the actual error/schema, then use a lighter alternate supported method. Do not immediately repeat an identical failed call; retry only after a meaningful method/input/condition change. Re-fetch authoritative state before repeating a mutation if the outcome is uncertain.
5. **Synchronize against the source of truth.** Before applying review feedback, re-fetch current PR HEAD SHA, latest review/comment timestamps, unresolved threads and CI results. After any push, require current-head review evidence and fresh CI for the new commit. When remote status is queued or stale, say so explicitly; do not infer success.
6. **Do useful parallel work, without making the review stale.** While waiting for CodeRabbit/CI, work on a bounded, relevant, non-conflicting task. Avoid pushing unrelated commits to the same PR while a review is processing; if an urgent fix changes HEAD, record the new SHA and re-check/re-request review.
7. **Bound waiting and recovery.** Poll only at meaningful checkpoints and for a bounded number of attempts when a real wait/status-check facility is available. Do not claim background or real-time work after the current run ends. If the review remains pending or the same blocker persists, continue independent work and report the last verified state and next action.
8. **Completion gate.** Report a task as complete only after verifying the saved output, relevant tests/checks, current review/CI state and remaining blockers. Keep versioned backups until the new version is checked. Never fabricate file saves, test results, uploads, checksums or completion.

## What this does and does not do

- GitHub Actions supplies event-driven CI checks. The daily schedule runs the existing
  `Calculator Source CI` workflow only: syntax checks, regression tests, web build, and
  generated-output validation. It does **not** inspect open pull requests, CodeRabbit
  review status, review comments, or unresolved threads.
- The separate scheduled ChatGPT task may perform periodic project/review follow-up when
  it actually runs. It is not triggered by a CodeRabbit email or review event and is not
  a real-time watcher; a scheduled CI pass must never be treated as evidence that review
  comments were inspected. When no dedicated event-triggered review agent is configured,
  review inspection happens only during the scheduled task run or a manual check.
- CodeRabbit's review is an external AI review, not a direct message into the currently
  open ChatGPT conversation. Every follow-up run must fetch the latest PR head SHA,
  reviews, comments, and unresolved threads; compare the review evidence to the current
  head; verify and fix valid findings on the same PR branch; then report the resulting
  CI status. Do not say this work was done just because CodeRabbit emailed a notification.
- This loop is deliberately bounded. Automated reviews can be wrong, so owner approval
  remains required before merging.

## Validation of this setup

- PR #1 merged into `main` on 2026-10-22; its post-merge `Calculator Source CI` run
  passed: https://github.com/mrfahimhawladar/calculator-android-app/actions/runs/39785221025
- PR #1's CodeRabbit message said “Review skipped,” so this did not verify a full review.
- PR #2 is a test PR adding a regression case for exponentiation overflow. Its review
  must be tied to the exact current PR head SHA; a review of an earlier commit is stale.
  Re-check the SHA and CI after every push:
  https://github.com/mrfahimhawladar/calculator-android-app/pull/2
- Do not merge PR #2 automatically. Use it to confirm that a real AI review is produced
  and to check the regression test.

## Guidance for forks/new repositories

1. Install the CodeRabbit GitHub App from https://github.com/marketplace/coderabbitai
   and grant access to the target repository.
2. Keep `.coderabbit.yaml` and `AGENTS.md` in the default branch.
3. Open a small pull request and verify that CI passes and CodeRabbit produces an actual
   review submission/findings—not just a “review skipped” status.
