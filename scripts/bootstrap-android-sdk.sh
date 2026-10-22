#!/usr/bin/env bash
set -Eeuo pipefail
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Android/Sdk}}"
command -v java >/dev/null 2>&1 || { echo 'JDK is required.' >&2; exit 2; }
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
if [[ ! -x "$SDKMANAGER" ]]; then
  if command -v sdkmanager >/dev/null 2>&1; then SDKMANAGER="$(command -v sdkmanager)"
  else
    echo "sdkmanager is missing. Install official Android command-line tools into: $SDK_ROOT" >&2
    echo 'Official guide: https://developer.android.com/tools/sdkmanager' >&2
    exit 3
  fi
fi
export ANDROID_HOME="$SDK_ROOT" ANDROID_SDK_ROOT="$SDK_ROOT"
mkdir -p "$SDK_ROOT"
if [[ "${ANDROID_SDK_ACCEPT_LICENSES:-}" == yes ]]; then
  set +o pipefail
  yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null
  set -o pipefail
elif [[ -t 0 ]]; then
  "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses
else
  echo 'License acceptance requires a person to review and accept the Android SDK licenses. Run interactively or set ANDROID_SDK_ACCEPT_LICENSES=yes only after personally agreeing.' >&2
  exit 4
fi
"$SDKMANAGER" --sdk_root="$SDK_ROOT" --install 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools' 'cmdline-tools;latest'
for required in "$SDK_ROOT/platforms/android-35/android.jar" "$SDK_ROOT/build-tools/35.0.0/apksigner" "$SDK_ROOT/build-tools/35.0.0/aapt" "$SDK_ROOT/platform-tools/adb"; do
  [[ -s "$required" || -x "$required" ]] || { echo "Required SDK file missing: $required" >&2; exit 5; }
done
printf 'ANDROID_SDK_BOOTSTRAP_RESULT=SUCCESS\nANDROID_SDK_ROOT=%s\n' "$SDK_ROOT"
