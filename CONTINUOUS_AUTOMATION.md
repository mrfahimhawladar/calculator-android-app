# Continuous Automation: Best-Effort 24/7 Coverage

## What is now implemented

The `.github/workflows/continuous-quality-loop.yml` workflow adds three layers:

1. **Event-driven checks:** runs on pushes to `main`, pull-request updates, submitted PR reviews, and new inline review comments.
2. **Hourly health pulse:** runs regression tests and a web build against `main` even if no PR/review event happens.
3. **Manual recovery:** `workflow_dispatch` lets an authorized maintainer re-run the same verification.

Each run installs dependencies, runs `npm test`, runs `npm run build`, verifies generated output, and records the exact tested SHA plus outcomes in the GitHub Actions summary. Runs are bounded to 15 minutes and newer runs can cancel stale runs for the same ref.

## What this does and does not mean

This is a real hosted automation attempt, not just a prompt saying continuous work is impossible. GitHub Actions starts work automatically when matching events arrive and also performs an hourly check. It does **not** keep one process alive 24/7, guarantee exact scheduling, or autonomously author code fixes. Scheduled events may be delayed, and hosted runners are ephemeral.

For autonomous code repair, add an explicitly approved coding-agent integration with a narrowly scoped repository credential/model secret and a separate review/test gate. Until those credentials and permissions are verified, the workflow still keeps deterministic tests/build checks running and exposes the latest results. Never place tokens in source code or logs, never auto-merge, and never treat a green check as proof that every review finding has been fixed.

## Operational verification

After opening the PR:
- Confirm the workflow is enabled in GitHub Actions.
- Check one PR/review-triggered run and one scheduled run when available.
- Confirm that each run summary reports the exact commit SHA and test/build outcomes.
- If Actions is disabled or repository policy blocks a trigger, fix that configuration or document the exact permission blocker rather than abandoning the automation attempt.
