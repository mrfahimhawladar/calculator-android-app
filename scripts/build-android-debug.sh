#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
fail(){ printf '\nANDROID BUILD BLOCKED: %s\n' "$1" >&2; exit 2; }
network_fail(){ printf '\nANDROID BUILD STOPPED BEFORE NEXT DOWNLOAD: %s\n' "$1" >&2; echo 'This script cannot repair platform DNS/egress restrictions; use an internet-enabled runner if bounded retries fail.' >&2; exit 3; }
for c in node npm java curl unzip sha256sum; do command -v "$c" >/dev/null 2>&1 || fail "$c is required"; done
java -version >/dev/null 2>&1 || fail 'Java could not start.'
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/.cache/calculator/android-sdk}}"
export ANDROID_HOME="$SDK_ROOT" ANDROID_SDK_ROOT="$SDK_ROOT"
find_sdkmanager(){
 local cand
 [[ -x "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" ]] && { printf '%s\n' "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"; return 0; }
 for cand in "$SDK_ROOT"/cmdline-tools/*/bin/sdkmanager; do [[ -x "$cand" ]] && { printf '%s\n' "$cand"; return 0; }; done
 return 1
}
SDKMANAGER="$(find_sdkmanager || true)"
SDK_READY=1
for f in platforms/android-35/android.jar platform-tools/adb build-tools/35.0.0/aapt build-tools/35.0.0/apksigner; do [[ -e "$SDK_ROOT/$f" ]] || SDK_READY=0; done
if [[ "$SDK_READY" != 1 ]]; then
 if [[ "${CALCULATOR_AUTO_INSTALL_ANDROID_SDK:-1}" == 0 ]]; then fail "Android SDK API 35 / Build Tools 35.0.0 / Platform Tools are incomplete at $SDK_ROOT and auto-install is disabled."; fi
 # The bootstrap helper can install the official command-line tools if sdkmanager is absent.
 bash scripts/bootstrap-android-sdk.sh
 SDKMANAGER="$(find_sdkmanager || true)"
fi
if [[ ! -x "$ROOT/node_modules/.bin/cap" || ! -d "$ROOT/node_modules/@capacitor/core" || ! -d "$ROOT/node_modules/@capacitor/android" ]]; then
 echo 'Capacitor dependencies are missing; preflighting npm before install.'
 node - <<'NODE'
const dns=require('node:dns').promises;
(async()=>{for(let n=1;n<=4;n++){try{const r=await dns.lookup('registry.npmjs.org');console.log('[DNS OK] registry.npmjs.org',r.address);process.exit(0)}catch(e){console.error('[DNS RETRY]',n,e.code||e.message);if(n===4)process.exit(3);await new Promise(r=>setTimeout(r,n*1500))}}})();
NODE
 rc=$?; [[ "$rc" == 0 ]] || network_fail 'npm registry DNS remained unavailable before npm install.'
 curl --fail --silent --show-error --head --location --retry 2 --retry-all-errors --retry-delay 2 --retry-max-time 15 --connect-timeout 5 --max-time 20 https://registry.npmjs.org/ -o /dev/null || network_fail 'npm registry HTTPS preflight failed.'
 npm_config_fetch_retries=2 npm_config_fetch_retry_mintimeout=1000 npm_config_fetch_retry_maxtimeout=8000 npm install --no-audit --no-fund || network_fail 'npm install failed after bounded retries.'
fi
npm test
npm run build
if [[ ! -d android ]]; then npx cap add android; fi
[[ -s android/gradle/wrapper/gradle-wrapper.properties ]] || fail 'Gradle wrapper properties missing after Capacitor Android project creation.'
DIST_RAW="$(sed -n 's/^distributionUrl=//p' android/gradle/wrapper/gradle-wrapper.properties | tail -n 1)"
DIST_URL="$(printf '%s' "$DIST_RAW" | sed 's/\\:/:/g')"
[[ "$DIST_URL" == https://* ]] || fail "Gradle wrapper distribution URL is invalid: ${DIST_RAW:-<missing>}"
echo "Preflighting exact Gradle distribution: $DIST_URL"
for host in services.gradle.org repo.maven.apache.org maven.google.com; do
 node - "$host" <<'NODE'
const dns=require('node:dns').promises; const h=process.argv[2];
(async()=>{try{const r=await dns.lookup(h);console.log('[DNS OK]',h,r.address)}catch(e){console.error('[DNS BLOCKED]',h,e.code||e.message);process.exit(3)}})();
NODE
 rc=$?; [[ "$rc" == 0 ]] || network_fail "DNS unavailable for $host before Gradle."
done
for url in "$DIST_URL" https://repo.maven.apache.org/maven2/ https://maven.google.com/; do
 curl --fail --silent --show-error --head --location --retry 2 --retry-all-errors --retry-delay 2 --retry-max-time 15 --connect-timeout 5 --max-time 20 "$url" -o /dev/null || network_fail "Gradle/Maven HTTPS preflight failed: $url"
done
if [[ -n "$SDKMANAGER" ]]; then export PATH="$(dirname "$SDKMANAGER"):$SDK_ROOT/platform-tools:$PATH"; else export PATH="$SDK_ROOT/platform-tools:$PATH"; fi
npx cap sync android
[[ -f android/gradlew ]] || fail 'android/gradlew is missing after Capacitor sync.'
chmod +x android/gradlew
APK="$ROOT/android/app/build/outputs/apk/debug/app-debug.apk"
mkdir -p "$(dirname "$APK")"
rm -f -- "$APK"
APKSIGNER="$SDK_ROOT/build-tools/35.0.0/apksigner"
AAPT="$SDK_ROOT/build-tools/35.0.0/aapt"
[[ -x "$APKSIGNER" ]] || fail "apksigner is missing: $APKSIGNER"
[[ -x "$AAPT" ]] || fail "aapt is missing: $AAPT"
(cd android && ./gradlew --no-daemon --stacktrace assembleDebug)
[[ -s "$APK" ]] || fail "Gradle did not produce $APK"
"$APKSIGNER" verify --verbose "$APK"
unzip -t "$APK" >/dev/null
ENTRIES="$(unzip -Z1 "$APK")"
grep -Fx 'AndroidManifest.xml' <<<"$ENTRIES" >/dev/null || fail 'APK has no AndroidManifest.xml.'
grep -E '^classes([0-9]+)?[.]dex$' <<<"$ENTRIES" >/dev/null || fail 'APK has no classes*.dex.'
BADGING="$("$AAPT" dump badging "$APK")"
[[ "$BADGING" == *"package: name='app.calculator.offline'"* ]] || fail 'APK package ID mismatch.'
[[ "$BADGING" == *"application-label:'Calculator'"* ]] || fail 'APK app label mismatch.'
sha256sum "$APK"
ls -lh "$APK"
printf '\nAPK_BUILD_RESULT=SUCCESS\nAPK_TYPE=DEBUG_TESTING_SIDELOAD_ONLY\nAPK_PACKAGE_ID=app.calculator.offline\nAPK_LABEL=Calculator\nAPK_ARCHIVE_INTEGRITY=PASS\nAPK_ANDROID_MANIFEST=PASS\nAPK_CLASSES_DEX=PASS\nAPK_PACKAGE_AND_LABEL=PASS\nAPK_SIGNATURE_VERIFICATION=PASS\nAPK_PATH=%s\n' "$APK"
BADGING="$("$AAPT" dump badging "$APK")"
[[ "$BADGING" == *"package: name='app.calculator.offline'"* ]] || fail 'APK package ID mismatch.'
[[ "$BADGING" == *"application-label:'Calculator'"* ]] || fail 'APK app label mismatch.'
sha256sum "$APK"
ls -lh "$APK"
printf '\nAPK_BUILD_RESULT=SUCCESS\nAPK_TYPE=DEBUG_TESTING_SIDELOAD_ONLY\nAPK_PACKAGE_ID=app.calculator.offline\nAPK_LABEL=Calculator\nAPK_ARCHIVE_INTEGRITY=PASS\nAPK_ANDROID_MANIFEST=PASS\nAPK_CLASSES_DEX=PASS\nAPK_PACKAGE_AND_LABEL=PASS\nAPK_SIGNATURE_VERIFICATION=PASS\nAPK_PATH=%s\n' "$APK"
