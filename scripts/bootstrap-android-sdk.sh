#!/usr/bin/env bash
set -Eeuo pipefail
TOOLS_VERSION='15859902'
TOOLS_URL="https://dl.google.com/android/repository/commandlinetools-linux-${TOOLS_VERSION}_latest.zip"
TOOLS_SHA256='4e4c464f145a7512b57d088ac6c278c03c9eea610886b35a5e0804e74eedf583'
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/.cache/calculator/android-sdk}}"
fail(){ printf 'ANDROID SDK BOOTSTRAP FAILED: %s\n' "$1" >&2; exit 3; }
[[ "$(uname -s)" == Linux && "$(uname -m)" == x86_64 ]] || fail 'Automatic installer supports Linux x86_64 only.'
for c in java curl unzip sha256sum; do command -v "$c" >/dev/null 2>&1 || fail "Missing command: $c"; done
java -version >/dev/null 2>&1 || fail 'Java cannot start.'
find_sdkmanager(){
 local cand
 [[ -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]] && { printf '%s\n' "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"; return 0; }
 for cand in "$SDK_ROOT"/cmdline-tools/*/bin/sdkmanager; do [[ -x "$cand" ]] && { printf '%s\n' "$cand"; return 0; }; done
 return 1
}
SDKMANAGER="$(find_sdkmanager || true)"
if [[ -n "$SDKMANAGER" && -x "$SDK_ROOT/platform-tools/adb" && -x "$SDK_ROOT/build-tools/35.0.0/apksigner" && -x "$SDK_ROOT/build-tools/35.0.0/aapt" && -f "$SDK_ROOT/platforms/android-35/android.jar" ]]; then
 printf 'ANDROID_SDK_BOOTSTRAP_RESULT=ALREADY_READY\nANDROID_SDK_ROOT=%s\n' "$SDK_ROOT"; exit 0
fi
check_url(){ curl --fail --silent --show-error --head --location --retry 2 --retry-all-errors --retry-delay 2 --retry-max-time 15 --connect-timeout 5 --max-time 20 "$1" -o /dev/null; }
if [[ -z "$SDKMANAGER" ]]; then
 echo "Checking official Android SDK endpoint before downloading: $TOOLS_URL"
 check_url https://dl.google.com/android/repository/repository2-1.xml || fail 'Android SDK repository unreachable; no tool archive was downloaded.'
 check_url "$TOOLS_URL" || fail 'Official command-line tools archive URL unreachable; no download attempted.'
 mkdir -p "$SDK_ROOT"
 TMP="$(mktemp -d)"; trap 'rm -rf "$TMP"' EXIT
 ZIP="$TMP/commandlinetools.zip"
 curl --fail --location --retry 2 --retry-all-errors --retry-delay 2 --retry-max-time 45 --connect-timeout 8 --max-time 600 "$TOOLS_URL" --output "$ZIP" || fail 'Official Android tools download failed.'
 ACTUAL="$(sha256sum "$ZIP" | awk '{print $1}')"
 [[ "$ACTUAL" == "$TOOLS_SHA256" ]] || fail "Official tools SHA-256 mismatch: $ACTUAL"
 mkdir -p "$TMP/unpack" "$SDK_ROOT/cmdline-tools"
 unzip -q "$ZIP" -d "$TMP/unpack"
 [[ -x "$TMP/unpack/cmdline-tools/bin/sdkmanager" ]] || fail 'Downloaded archive does not contain sdkmanager.'
 rm -rf "$SDK_ROOT/cmdline-tools/.latest-staging"
 mkdir -p "$SDK_ROOT/cmdline-tools/.latest-staging"
 cp -a "$TMP/unpack/cmdline-tools/." "$SDK_ROOT/cmdline-tools/.latest-staging/"
 rm -rf "$SDK_ROOT/cmdline-tools/latest.previous"
 [[ ! -d "$SDK_ROOT/cmdline-tools/latest" ]] || mv "$SDK_ROOT/cmdline-tools/latest" "$SDK_ROOT/cmdline-tools/latest.previous"
 mv "$SDK_ROOT/cmdline-tools/.latest-staging" "$SDK_ROOT/cmdline-tools/latest"
 rm -rf "$SDK_ROOT/cmdline-tools/latest.previous"
 SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
fi
export ANDROID_HOME="$SDK_ROOT" ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$(dirname "$SDKMANAGER"):$SDK_ROOT/platform-tools:$PATH"
echo "SDK target: $SDK_ROOT"
if [[ "${ANDROID_SDK_ACCEPT_LICENSES:-}" == yes ]]; then
 set +o pipefail
 yes | "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses >/dev/null
 set -o pipefail
elif [[ -t 0 ]]; then
 "$SDKMANAGER" --sdk_root="$SDK_ROOT" --licenses
else
 fail 'SDK license acceptance requires explicit user action. Review terms and run sdkmanager --licenses interactively, or set ANDROID_SDK_ACCEPT_LICENSES=yes only after personally agreeing.'
fi
"$SDKMANAGER" --sdk_root="$SDK_ROOT" --install 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools' 'cmdline-tools;latest'
for f in "$SDK_ROOT/platform-tools/adb" "$SDK_ROOT/build-tools/35.0.0/apksigner" "$SDK_ROOT/build-tools/35.0.0/aapt" "$SDK_ROOT/platforms/android-35/android.jar"; do [[ -s "$f" || -x "$f" ]] || fail "SDK Manager finished without required file: $f"; done
printf 'ANDROID_SDK_BOOTSTRAP_RESULT=SUCCESS\nANDROID_SDK_ROOT=%s\n' "$SDK_ROOT"
