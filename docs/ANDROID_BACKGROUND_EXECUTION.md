# Android Background Execution Guidelines — Battery, Memory & Reliability

Updated: 2026-10-22. This is an architecture guide, not a claim that a background service has been added to the Calculator APK.

## Core decision

The Calculator's main function is local, offline-first calculation. It has no demonstrated need to keep a CPU wake lock or an always-on foreground service after the user leaves the calculator. **Do not add a permanent background service to this app merely to prevent process death or keep CodeRabbit/ChatGPT alive.** Run repository automation on GitHub/authorized infrastructure, not in the calculator app's process.

Android does not offer a general “unkillable app” permission. `START_STICKY`, alarms, boot receivers, battery settings, and a foreground notification improve particular use cases but do not guarantee the process will never be killed.

## Select the right API

1. **Deferrable/persistent tasks:** use WorkManager. Its periodic interval is at least 15 minutes, and execution time remains inexact and system-managed. Good for sync/cleanup that may run later.
2. **Immediate, bounded work:** use an expedited one-time WorkRequest when work must start immediately. For a user-initiated data transfer that needs progress notification, use a user-initiated data-transfer job on API 34+; use regular WorkManager for short, interruptible transfers.
3. **Ongoing, user-visible work that should not be interrupted:** use a foreground service only when the task qualifies, started from a legitimate user-visible flow, with an ongoing notification and the correct service type/permissions.
4. **Long-running AI/code review:** use the GitHub app/workflows or a separately deployed authorized runner. A handset service should not try to keep a chat model, CodeRabbit, or an account's memory “alive”.

Reference: https://developer.android.com/develop/background-work/background-tasks/persistent

## Android 12–16 restrictions

- Apps targeting Android 12/API 31+ generally cannot start a foreground service while already in the background, except for defined exemptions.
- Apps targeting Android 14/API 34+ must declare the appropriate foreground-service type and corresponding permissions; mismatches can cause a `SecurityException`.
- For apps targeting API 35+, Android 15 limits `dataSync` and `mediaProcessing` foreground services to six hours in any 24-hour period while the app is in the background. Each type has separate time accounting. Bringing the app to the foreground resets the timer. The service must handle `onTimeout` and stop promptly when the limit is reached.
- `BOOT_COMPLETED` does not waive foreground-service restrictions. **Apps targeting Android 15/API 35+** cannot start several foreground-service types (including `dataSync`, `mediaPlayback`, `camera`, `phoneCall`, `mediaProjection`, and `microphone`) from a `BOOT_COMPLETED` receiver; the exact restriction depends on target SDK and service type.
- Android 16/API 36: jobs started while a foreground service is running—including WorkManager, JobScheduler and DownloadManager jobs—still obey their respective runtime quotas regardless of target SDK. A foreground service is not a quota bypass. Checkpoint long tasks, make them resumable/idempotent, and inspect `WorkInfo.getStopReason()` or `JobParameters.getStopReason()` when work is stopped.
- `START_STICKY` is best-effort restart behavior, not a guarantee of continuous runtime.
- Only declare the type that accurately describes the real task. Do not choose `specialUse` as a generic loophole.

References:
- https://developer.android.com/develop/background-work/services/fgs/launch
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/services/fgs/changes
- https://developer.android.com/topic/performance/power/power-details
- https://developer.android.com/develop/background-work/services/fgs/timeout

## Permissions and battery optimization

Request only what the actual feature requires. For a legitimate foreground service this may include `FOREGROUND_SERVICE`, the specific type permission and Android 13+ notification permission handling. Add `WAKE_LOCK` only if a tested, justified operation truly requires it. Do not add `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_SPECIAL_USE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, or `RECEIVE_BOOT_COMPLETED` speculatively. `specialUse` is a service type; the corresponding manifest permission is `FOREGROUND_SERVICE_SPECIAL_USE`, and the service must document its specific use case in a manifest property.

Do **not** auto-open or automatically grant battery-optimization exemptions. An app can open the general battery-optimization settings for the user; direct `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is appropriate only for narrow eligible use cases where Doze/App Standby materially breaks the app's core function and must respect current store policy. A calculator has not established such a need.

OEM settings (Xiaomi/POCO, Samsung, Huawei, OPPO, vivo) vary by OS version and region. If an actual feature is legitimately affected, explain the available user-controlled battery/autostart settings and let the user choose; never claim an OEM “bypass” is guaranteed.

Reference: https://developer.android.com/training/monitoring-device-state/doze-standby

## Wake-lock rule

A `PARTIAL_WAKE_LOCK` holds the CPU awake and can materially reduce battery life. It is not a heartbeat and must not be renewed forever.

- Prefer WorkManager, network constraints and resumable/idempotent operations.
- Acquire a wake lock only for the shortest critical section that genuinely needs it; use a timeout and release it in `finally`.
- Never hold it while idle, waiting for CodeRabbit, sleeping between heartbeat checks, or doing no work.
- Record acquisition/release metrics and investigate excessive wake-lock duration.

Illustrative pattern, only for a justified bounded operation:

```kotlin
val wakeLock = powerManager.newWakeLock(
    PowerManager.PARTIAL_WAKE_LOCK,
    "$packageName:shortCriticalWork"
)
wakeLock.acquire(60_000L) // hard upper bound; use only if genuinely required
try {
    performShortBoundedOperation()
} finally {
    if (wakeLock.isHeld) wakeLock.release()
}
```

The manifest would need `android.permission.WAKE_LOCK` for this pattern. It must not be added unless the code path is real and tested.

Reference: https://developer.android.com/develop/background-work/background-tasks/awake/wakelock/best-practices

## Foreground-service manifest example (conditional, not for the current calculator by default)

Only when WorkManager and a user-initiated data-transfer job are not suitable, and the user-initiated sync meets the `dataSync` foreground-service requirements and time limits:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<!-- Android 13+: request POST_NOTIFICATIONS at runtime as appropriate for the app UX. -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<application ...>
    <service
        android:name=".UserInitiatedSyncService"
        android:exported="false"
        android:foregroundServiceType="dataSync" />
</application>
```

This example is not a complete production manifest. The service must create a notification channel, promptly call `ServiceCompat.startForeground(...)` with a valid notification, perform cancellable bounded work, handle failures/timeouts, and stop itself when complete. The UI must expose an explicit start/stop control. For Android 14+, confirm the permission/type match; for Android 15+, observe data-sync time limits and boot-start restrictions.

## Memory and cache policy

- Use bounded queues and bounded in-memory caches; clear/release unused bitmaps, cursors and large collections when appropriate.
- Respond to memory pressure with appropriate trim callbacks and recreate caches lazily.
- Persist essential work state in compact storage; do not keep full logs or every prior task result in RAM.
- Rotate/cap diagnostic logs and redact secrets/personal data. Store summaries plus IDs/links to complete remote logs.
- Do not rely on `System.gc()` or periodic forced cache clearing as an OOM solution.
- Make background work idempotent, restartable and checkpointed; tolerate process death and duplicate execution.
- Avoid tight heartbeat loops. Use event-driven GitHub events and a modest scheduled check; check the current commit SHA before accepting any review as fresh.

## Decision for this Calculator project

Do not implement a 24/7 foreground service in the current app without an approved, user-visible functional requirement and reviewed resource budget. The existing GitHub Continuous Quality Loop + daily ChatGPT task are the correct first layers. A true autonomous coding agent still needs a separate authorized host/integration and must remain observable, bounded, secure and stoppable.
