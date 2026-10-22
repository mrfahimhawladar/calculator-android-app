#!/usr/bin/env bash
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
BAD=0
ok(){ printf '[AVAILABLE] %s\n' "$1"; }
miss(){ printf '[NOT FOUND] %s\n' "$1"; BAD=1; }
block(){ printf '[BLOCKED] %s\n' "$1"; BAD=1; }
printf 'Calculator Android Build Doctor\nTimestamp UTC: '; date -u '+%Y-%m-%dT%H:%M:%SZ'
printf 'Workspace: %s\n\n' "$ROOT"
for c in bash node npm java javac curl unzip sha256sum git; do
 if command -v "$c" >/dev/null 2>&1; then ok "$c: $(command -v "$c")"; else miss "$c executable"; fi
done
printf '\n=== Runtime versions ===\n'
node --version 2>&1 || true
npm --version 2>&1 || true
java -version 2>&1 | head -2 || true
javac -version 2>&1 || true
printf '\n=== Gradle / Android project ===\n'
if command -v gradle >/dev/null 2>&1; then gradle --version 2>&1 | head -3; else miss 'system Gradle is not in PATH'; fi
for f in android/gradlew android/gradle/wrapper/gradle-wrapper.jar android/gradle/wrapper/gradle-wrapper.properties; do
 [[ -s "$ROOT/$f" ]] && ok "$f" || miss "$f"
done
printf '\n=== Android SDK ===\n'
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -n "$SDK_ROOT" && -d "$SDK_ROOT" ]]; then
 ok "SDK root: $SDK_ROOT"
 for f in cmdline-tools/latest/bin/sdkmanager platform-tools/adb platforms/android-35/android.jar build-tools/35.0.0/aapt build-tools/35.0.0/aapt2 build-tools/35.0.0/apksigner; do
  [[ -e "$SDK_ROOT/$f" ]] && ok "$SDK_ROOT/$f" || miss "$SDK_ROOT/$f"
 done
else
 miss 'ANDROID_HOME / ANDROID_SDK_ROOT does not point to an existing SDK directory'
 for f in "$HOME/Android/Sdk" "$HOME/.android/sdk" "$HOME/.cache/calculator/android-sdk" /opt/android-sdk /opt/android-sdk-linux /usr/lib/android-sdk; do
  [[ -d "$f" ]] && printf '[SDK CANDIDATE] %s\n' "$f"
 done
fi
printf '\n=== Dependency caches ===\n'
for d in "$HOME/.npm" "$HOME/.gradle" "$HOME/.android" "$ROOT/node_modules" "$ROOT/android"; do
 [[ -d "$d" ]] && du -sh "$d" 2>/dev/null | sed 's/^/[AVAILABLE] /' || printf '[NOT FOUND] %s\n' "$d"
done
printf '\n=== DNS / download hosts ===\n'
if node - <<'NODE'
const dns=require('node:dns').promises;
(async()=>{let failed=false;for(const host of ['registry.npmjs.org','dl.google.com','services.gradle.org','github.com','repo.maven.apache.org','maven.google.com','release-assets.githubusercontent.com','objects.githubusercontent.com']){try{const r=await dns.lookup(host);console.log('[DNS OK]',host,r.address)}catch(e){console.log('[DNS BLOCKED]',host,e.code||e.message);failed=true}}if(failed)process.exitCode=1})();
NODE
then :; else BAD=1; fi
for url in https://registry.npmjs.org/ https://dl.google.com/android/repository/repository2-1.xml https://services.gradle.org/ https://repo.maven.apache.org/maven2/ https://maven.google.com/; do
 if curl -fsSI --connect-timeout 5 --max-time 12 "$url" >/dev/null 2>&1; then ok "HTTPS $url"; else block "HTTPS unavailable: $url"; fi
done
printf '\n=== Summary ===\n'
if [[ "$BAD" -eq 0 ]]; then echo 'RESULT=PREREQUISITES_LOOK_AVAILABLE'; exit 0; fi
echo 'RESULT=ANDROID_BUILD_PREFLIGHT_FAILED'
echo 'No APK build is claimed by this diagnostic.'
exit 3
