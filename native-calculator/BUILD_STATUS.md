# Calculator Native Build Status

Date: 2026-11-24
Branch: `native-calculator-id-test`
App label: `Calculator`
Current application ID: `com.calculator`
Namespace: `app.calculator.offline`

## Verified results

- JDK 17 setup: PASS
- Gradle 9.6.0 setup: PASS
- Android SDK API 36 and Build Tools 36 setup: PASS
- Android SDK license acceptance for this Calculator build: PASS
- Compose BOM 2025.12.00 dependency metadata check: PASS

## Previous build attempt

Run 45375949082 reached Android manifest processing. Gradle explicitly rejected the requested bare ID:
`Package name 'Calculator' ... should contain at least one '.'`.
Kotlin compilation also reported a Compose `weight` import issue and an Experimental Material3 opt-in requirement. These three issues are corrected in the current commit.

## Next

Run unit tests and assemble debug APK using `com.calculator`. Verify the APK package ID, signature and ZIP integrity before sharing an artifact.

The Android Developer Console entry previously shown as `Calculator` does not match the corrected Android application ID. Register `com.calculator` with the already-verified signing certificate before distribution. Do not generate or replace a signing key.
