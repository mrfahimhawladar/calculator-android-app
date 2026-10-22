# AI Review and Feedback Loop

## Purpose

Use independent review and automated checks to catch defects before calculator changes
are merged. The target is a bounded feedback loop, not an endlessly running or
self-merging AI process.

## Required change flow

1. Make each code change on a topic branch and open or update a pull request to `main`.
   Do not make normal application-code changes directly to `main`.
2. The existing `Calculator Source CI` workflow runs on pull requests and pushes to
   `main`, and daily at 19:00 Bangladesh time (13:00 UTC). It performs JavaScript/shell
   syntax checks, regression tests, and the web build.
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

## What this does and does not do

- GitHub Actions supplies event-driven checks; the scheduled task supplies periodic
  follow-up work. Neither is a continuously thinking ChatGPT session.
- CodeRabbit's review is an external AI review, not a direct message into the currently
  open ChatGPT conversation. The next automation run must explicitly read and evaluate
  its comments.
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
