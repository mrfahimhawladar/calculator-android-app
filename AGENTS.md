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
