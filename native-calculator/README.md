# Calculator — Native Android

Native Kotlin + Jetpack Compose calculator.

- App label: `Calculator`
- Application ID: `com.calculator` (changed after Gradle rejected the bare identifier `Calculator`)
- Namespace: `app.calculator.offline`
- AGP: `9.3.1`
- Gradle CI: `9.6.0`
- Kotlin Compose plugin: `2.4.21`
- Compose BOM: `2025.12.00`
- Compile SDK: 36
- Min SDK: 26
- Target SDK: 36

The old Android Developer Console entry displayed as `Calculator`; this is not a valid Android application ID. Register `com.calculator` in the Android Developer Console using the same signing certificate before distribution. Keep the verified signing key unchanged.

Build task: `:app:testDebugUnitTest :app:assembleDebug`
