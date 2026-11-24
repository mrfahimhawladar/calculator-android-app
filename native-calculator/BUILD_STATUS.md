# Calculator Native Build Status

Date: 2026-11-24
Branch: `native-calculator-id-test`
Application ID trial: `Calculator` (unchanged)
Namespace: `app.calculator.offline`
App label: `Calculator`

Source files have been uploaded to this branch. The build workflow is configured to run on the final workflow-file push. It uses AGP 9.3.1, Kotlin Compose plugin 2.4.21 and Gradle 9.6.0; this is an aligned toolchain for API 37 based on current compatibility tables.

The user authorized Android SDK license acceptance for this Calculator build. The workflow accepts the SDK licenses, installs API 37/build tools 36.0.0, runs unit tests, attempts the APK build, and verifies the package ID and signature if compilation succeeds.

No APK is claimed before a successful workflow run and artifact verification.
