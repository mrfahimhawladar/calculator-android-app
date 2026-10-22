# AI Review and Feedback Loop

## Purpose

Use independent review and automated checks to catch defects before calculator changes
are merged. The target is a bounded feedback loop, not an endlessly running or
self-merging AI process.

## Required change flow

1. Make each code change on a topic branch and open or update a pull request to `main`.
   Do not make normal application-code changes directly on `main`.
2. The `Calculator CI` workflow runs on pull requests and checks package metadata,
   shell syntax, regression tests, and the web build. It also runs daily at 19:00
   Bangladesh time (13:00 UTC).
3. After the repository owner installs and enables the CodeRabbit GitHub App for this
   public repository, CodeRabbit reviews pull requests using `.coderabbit.yaml` and
   `AGENTS.md`. It can report inline findings and summarize risks.
4. At the next Calculator automation run, inspect open pull requests, check statuses,
   CodeRabbit review comments, and unresolved threads. Verify each finding against the
   current code, tests, and requirements before changing anything.
5. Fix valid defects in the same pull request, add regression tests, and wait for checks
   and review feedback to update. Re-review at most two correction passes per run; if
   significant issues remain, report them with links and stop for owner input.
6. Never auto-merge, suppress a failing check, fabricate a test result, or apply a review
   suggestion that weakens parser safety, offline-first behavior, or secret handling.
7. If CodeRabbit is not installed or cannot review the pull request, report that blocker.
   Continue with deterministic CI checks; do not claim that AI review occurred.

## What this does and does not do

- GitHub Actions supplies event-driven checks; the scheduled task supplies periodic
  follow-up work. Neither is a continuously thinking ChatGPT session.
- CodeRabbit's review is an external AI review, not a direct message into the currently
  open ChatGPT conversation. The next automation run must explicitly read and evaluate
  its comments.
- This loop is deliberately bounded. Automated reviews can be wrong, so owner approval
  remains required before merging.

## One-time owner setup

1. Install the CodeRabbit GitHub App from https://github.com/marketplace/coderabbitai
   and grant access to `mrfahimhawladar/calculator-android-app`.
2. Merge the pull request that introduces this setup.
3. Open a small test pull request and confirm that `Calculator CI` and CodeRabbit both
   report results. Then verify that the Calculator automation reads the review findings
   and CI status before making a follow-up change.
