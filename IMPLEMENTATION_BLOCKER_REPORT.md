# Calculator Android — Native APK Build Report

Updated: 2026-10-22 (Asia/Dhaka)
Repository: https://github.com/mrfahimhawladar/calculator-android-app
Main build commit: 9a5e6d78c1784f9ba8ff82d2fe9c348298fd05b1

## Result
**NATIVE ANDROID DEBUG APK: BUILT AND VERIFIED — PASS**

GitHub Actions run: https://github.com/mrfahimhawladar/calculator-android-app/actions/runs/39773420832
Artifact download: https://github.com/mrfahimhawladar/calculator-android-app/actions/runs/39773420832/artifacts/12422734751
Artifact name: Calculator-debug-APK
Artifact ID: 12422734751
Artifact ZIP size: 3,705,894 bytes
Artifact retention expires: 2026-11-05T11:50:34Z

## Actual APK verification evidence
- Gradle: `BUILD SUCCESSFUL`
- APK signature: verified by `apksigner` (v1 and v2 schemes valid; one signer)
- ZIP integrity: PASS
- `AndroidManifest.xml`: PASS
- `classes*.dex`: PASS
- Package ID: `app.calculator.offline` — PASS
- Application label: `Calculator` — PASS
- SHA-256: `6486992e9df86b32c06bc9b51003a597d652f7985c66fa517b217bf8fa20bd26`
- The workflow generated and uploaded a SHA-256 sidecar with the APK.

A first build completed Gradle compilation and signing verification but exposed a typo in the DEX-check regex. That false rejection was fixed and the next build completed all checks and uploaded the artifact successfully.

## Source CI
Latest source CI results have passed dependency installation, JavaScript syntax, shell syntax, 18 regression tests, web build and generated output validation. Example latest successful CI: https://github.com/mrfahimhawladar/calculator-android-app/actions/runs/39773420894

## Remaining limitations
- This artifact is a **debug APK for testing/sideloading**, not a release-signed Play Store package.
- CI has not claimed a physical-device installation or device-specific smoke test.
- The one-time approval marker used to start the approved build has been removed; the Android workflow has returned to manual `workflow_dispatch` with explicit license-acknowledgement input.
- Any future build should use GitHub Actions and verify the actual artifact, not rely only on source CI.

## Security
No PAT was used or stored in project files. Never put PAT/OAuth tokens, passwords or signing keys in source, notebooks, logs, or configuration.
