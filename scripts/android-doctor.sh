#!/usr/bin/env bash
set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
bad=0
printf 'Calculator Android Build Doctor\nTimestamp UTC: '; date -u '+%Y-%m-%dT%H:%M:%SZ'
printf 'Workspace: %s\n\n' "$ROOT"
for cmd in node npm java curl unzip sha256sum git; do
  if command -v "$cmd" >/dev/null 2>&1; then printf '[OK] %-12s %s\n' "$cmd" "$(command -v "$cmd")"
  else printf '[MISSING] %s\n' "$cmd"; bad=1; fi
done
printf '\nNode: '; node --version 2>&1 || true
printf 'npm: '; npm --version 2>&1 || true
printf 'Java: '; java -version 2>&1 | head -1 || true
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" ]]; then printf '[BLOCKED] ANDROID_HOME / ANDROID_SDK_ROOT is not set\n'; bad=1
else
  printf 'SDK root: %s\n' "$SDK_ROOT"
  for path in platforms/android-35/android.jar build-tools/35.0.0/apksigner build-tools/35.0.0/aapt platform-tools/adb; do
    if [[ -e "$SDK_ROOT/$path" ]]; then printf '[OK] %s\n' "$SDK_ROOT/$path"; else printf '[MISSING] %s\n' "$SDK_ROOT/$path"; bad=1; fi
  done
fi
if command -v gradle >/dev/null 2>&1; then gradle --version | head -2; else printf '[INFO] system gradle is not required; Capacitor generates android/gradlew during build\n'; fi
printf '\nDNS preflight:\n'
node - <<'NODE'
const dns=require('node:dns').promises;
(async()=>{let bad=false;for(const h of ['registry.npmjs.org','dl.google.com','services.gradle.org','repo.maven.apache.org']){try{const r=await dns.lookup(h);console.log('[OK]',h,r.address)}catch(e){console.log('[BLOCKED]',h,e.code||e.message);bad=true}}if(bad)process.exitCode=3})();
NODE
rc=$?; if [[ $rc -ne 0 ]]; then bad=1; fi
if [[ "$bad" == 0 ]]; then echo 'RESULT=PREREQUISITES_PRESENT'; exit 0; fi
echo 'RESULT=BLOCKED_MISSING_PREREQUISITES_OR_NETWORK'; exit 3
