# Calculator — Native Android

Native Kotlin + Jetpack Compose calculator.

- App label: `Calculator`
- Requested Android application ID trial: `Calculator` (kept exactly as requested for the build test)
- Namespace: `app.calculator.offline`
- AGP: `9.3.1`
- Gradle CI: `9.6.0`
- Kotlin Compose plugin: `2.4.21`
- Compile SDK: 37
- Min SDK: 26
- Target SDK: 36

The application ID is intentionally not changed in this test branch. The CI workflow is authorized to accept the Android SDK license terms for this Calculator build. If the Android Gradle Plugin rejects the ID, the workflow log will preserve the exact error before any alternate ID is chosen.

Build task: `:app:testDebugUnitTest :app:assembleDebug`
