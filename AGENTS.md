# Instructions for coding agents — Calculator Android

## Project rules
- The repository root is the canonical project root. Do not nest the app under `Source_Root/` or `Repository_Publish_Pack/`.
- Keep the app offline-first. Do not add analytics, tracking, remote APIs, or secrets without explicit approval.
- Never place PATs, OAuth tokens, passwords, signing keys, or secrets in source files, issues, notebooks, build logs, or commits.
- Do not claim an APK exists unless the exact generated APK has been verified.
- Keep `web/` as editable browser source and regenerate `www/` through `npm run build`; `www/` is generated and ignored.
- Prefer small changes with regression tests. Preserve expression parsing safety; do not use `eval()` or `new Function()`.

## Required checks after changes
1. `npm test`
2. `npm run build`
3. `npm run android:doctor` when changing build or Android setup
4. For a claimed APK: run `npm run android:build:debug` and verify app ID `app.calculator.offline`, label `Calculator`, signature, ZIP structure, manifest, DEX and SHA-256.

## Android SDK license gate
The GitHub Actions workflow requires deliberate manual acknowledgement of Android SDK license terms. Do not bypass or silently approve this step. Enable the acknowledgement only after the operator reviews and agrees to the terms.
