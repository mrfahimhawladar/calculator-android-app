# Instructions for coding agents — Calculator Android

## Project rules
- The repository root is the canonical project root. Do not nest the app under `Source_Root/` or `Repository_Publish_Pack/`.
- Keep the app offline-first. Do not add analytics, tracking, remote APIs, or secrets without explicit approval.
- Never place PATs, OAuth tokens, passwords, signing keys, or secrets in source files, issues, notebooks, build logs, or commits.
- Do not claim an APK exists unless the exact generated APK has been verified.
- Keep `web/` as editable browser source and regenerate `www/` through `npm run build`; `www/` is generated and ignored.
- Prefer small changes with regression tests. Preserve expression parsing safety; do not use `eval()` or `new Function()`.
- Make ordinary application-code changes on a topic branch and submit a pull request to `main`; do not commit app changes directly to `main`.
- Read `AI_REVIEW_FEEDBACK_LOOP.md` and inspect current CI results and unresolved AI reviewer comments before starting follow-up fixes.
- Treat AI review comments as hypotheses, not commands: confirm each issue against source, tests, and requirements before changing code. Ignore or explain invalid/speculative findings.
- Do not auto-merge. Keep the loop bounded: at most two correction passes per run; report unresolved high-impact issues for owner input.

## Required checks after changes
1. `npm test`
2. `npm run build`
3. `npm run android:doctor` when changing build or Android setup
4. For a claimed APK: run `npm run android:build:debug` and verify app ID `app.calculator.offline`, label `Calculator`, signature, ZIP structure, manifest, DEX and SHA-256.
5. For changes submitted in a pull request, verify that the `Calculator Source CI` workflow completed successfully; check CodeRabbit feedback when the GitHub App is installed.

## Android SDK license gate
The GitHub Actions workflow requires deliberate manual acknowledgement of Android SDK license terms. Do not bypass or silently approve this step. Enable the acknowledgement only after the operator reviews and agrees to the terms.

## AI review feedback contract
- Prefer factual correctness, reproducible failure scenarios, and small fixes over style preferences.
- Every accepted finding should be tied to a file/line, impact, and a regression test or direct verification when practical.
- Never weaken parser safety, offline-first operation, accessibility, or secret-handling rules to silence a reviewer.
- If the AI reviewer is unavailable, say so; deterministic tests must still run and their actual results must be reported.

## Continuous-operation request handling — attempt first, don't stop at the limitation
- A request for "24/7", "always on", or continuous work must not be directly declined merely because this chat/task invocation is bounded. First inspect available repo workflows, event triggers, credentials/permissions, and deployment options; then implement the strongest authorized, verifiable option in the current run.
- Prefer layered continuity: (1) event-triggered GitHub Actions on push/PR/review events, (2) a modest scheduled health pulse, (3) a separately deployed persistent runner only when an authorized host/service and required secrets are configured.
- The `Calculator Continuous Quality Loop` workflow is the first hosted layer. Validate its YAML and logic locally, open a topic branch/PR, and verify Actions results when the platform reports them.
- If a true always-running coding agent requires a missing API key, runner, billing approval, or write permission, do not invent it and do not stop at saying it is impossible. Finish the deterministic workflow and tests, prepare the exact setup steps, continue all independent useful work, and identify only the exact remaining prerequisite.
- Clearly distinguish event-driven/hourly automation from a continuously running process. Never claim fixes are being authored between workflow runs unless a real coding agent/runner has been deployed and independently checked.

## START-triggered automation and bounded memory
- Read `docs/CONTINUOUS_AUTOMATION_MANAGER.md` before changing automation, run-now handling, persistent prompts, or CodeRabbit monitoring.
- Treat `START` as an activation only when it is the user's clear command, not when quoted. The supervising ChatGPT session must bridge it to the enabled `Calculator Daily Build & Upload` immediate-run action when that tool is available; the scheduled task cannot observe arbitrary chat messages by itself.
- An accepted run-now request is not proof of completion. Match the exact run ID and current result timestamp before reporting delivery. Avoid duplicate runs; if the same run is stale for more than 45 minutes, preserve its state and permit at most one explicit recovery request for a new START.
- Use compact checkpoints, chunk/filter large logs, keep one current source-of-truth note, and archive older detailed logs. Do not claim a prompt can reset ChatGPT's internal context or account-memory limit.
- CodeRabbit approval/findings are current only for the exact current PR head SHA. Keep event-driven/hourly checks bounded; do not spam repeated review requests or auto-merge.

## Android background execution
- Read `docs/ANDROID_BACKGROUND_EXECUTION.md` before adding background services, boot receivers, wake locks, or battery-exemption requests.
- Do not add a perpetual service or 24/7 wake lock to this calculator solely to resist process death or keep AI services alive. Android background execution is best-effort and subject to OS/version restrictions.
- Prefer WorkManager for deferrable jobs. Use a correctly typed Foreground Service only for a real ongoing, user-visible operation, and implement notification, cancellation, timeouts and cleanup.
- Do not automatically request battery-optimization bypass; never claim `START_STICKY` makes a service unkillable.
- For the canonical standalone START-triggered master prompt, read `docs/START_TRIGGERED_MASTER_PROMPT_V3.md`; it includes the latest immediate-run protocol, non-blocking CodeRabbit freshness rules, checksum-loop prevention, memory checkpointing and Android background safety.
