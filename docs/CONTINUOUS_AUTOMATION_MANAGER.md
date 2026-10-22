# Continuous Automation Manager, Context Budget & CodeRabbit Freshness

Updated: 2026-10-22 (Asia/Dhaka)

## 1. Architecture: separate the three kinds of continuity

**ChatGPT task continuity** is the enabled daily task. It performs one bounded execution when invoked; it cannot observe messages sent to another chat or keep this conversation permanently alive.

**Repository continuity** is GitHub Actions. The workflow `.github/workflows/continuous-quality-loop.yml` runs on: pushes to `main`; pull requests **targeting `main`** with `opened`, `synchronize`, `reopened`, `ready_for_review` or `edited`; submitted `pull_request_review` events; created `pull_request_review_comment` events; manual `workflow_dispatch`; and an hourly UTC schedule (`17 * * * *`). The hourly schedule is best-effort, not a precise clock or daemon.

**AI review continuity** is CodeRabbit's GitHub App. It reacts to eligible GitHub events; it is not a process that should be pinged every minute. CI can prove tests/build checks ran; it cannot prove that CodeRabbit reviewed the latest commit unless a review/response identifies that exact PR head.

These layers complement each other. Do not label any of them “infinite” or “unkillable”.

## 2. START / STOP protocol

Treat **START as an activation command only when it is clearly the user's command**, not when the word appears inside a pasted prompt, quotation, document, or explanation.

When the supervising ChatGPT session receives a standalone START:

1. If the task-management tool is available, inspect the enabled state and latest run of **Calculator Daily Build & Upload**. If the tool/action is unavailable, say exactly that task state cannot be inspected from this run; do not infer enabled state, run ID, start or completion.
2. If its state can be read and it is disabled, enable it only when the user has requested activation and the update action is available. If that action is unavailable, report the blocker and continue deterministic work only.
3. If a run is confirmed active and fresh, do not spawn duplicates. When the same run remains `in_progress` for more than 45 minutes without a matching result, mark it possibly stale, preserve its run ID and—because this is an explicit START—make at most one recovery request if the action is available. Avoid a tight retry loop.
4. Use the task's documented immediate-run action (`run_now`) once. “Request accepted” means only that a request was queued; it does not prove the worker finished or delivered anything.
5. Re-check task state. Accept a result as current only when its run ID matches the run being checked and its creation/completion times fit that run. If the result payload refers to an older run, call the latest run unverified and report the mismatch.
6. In the current conversation, start useful bounded work immediately as well when execution tools are available. Do not wait passively for the scheduled task or claim the scheduled worker finished before evidence arrives.

**STOP** means stop the current active work loop, preserve a checkpoint and do no further corrective iterations. **DISABLE AUTOMATION** means turn off the recurring task. A quoted “STOP” is not an activation/control request.

A ChatGPT task itself cannot subscribe to arbitrary user messages. The assistant receiving START must perform the documented run-now bridge. A prompt cannot grant itself new tool access or start itself from inside an inactive chat.

## 3. Bounded loop instead of a hot infinite loop

Each active run should repeat this finite cycle until its budget ends, STOP is received, or a real blocker occurs:

1. **Sync:** read the canonical root instruction and newest dated Project_Control handoff; get live GitHub main/PR heads, CI and review state.
2. **Select:** choose one highest-impact, independently verifiable task. Keep independent preparation relevant to pending reviews.
3. **Execute, if warranted:** make one small logical change on a topic branch and run applicable tests/build/checks. For verification-only runs or when blocked, do not create unnecessary branches/PRs; record a no-code-change checkpoint and continue any independent useful work.
4. **Reconcile:** re-fetch the exact current SHA, CI, latest review submissions and unresolved threads. Treat prior-head reviews as stale.
5. **Checkpoint:** record the compact source of truth, outcome, evidence, blocker and one next action.
6. Repeat only within the current task/time allowance. Maximum two correction passes per PR per run; never auto-merge.

Do not create a tight loop of bot comments, repeated API polling, repeated failed builds, or repeated uploads. Use the existing hourly workflow as the background heartbeat and event-driven triggers for fast response.

## 4. Memory and context budget

The platform controls ChatGPT's actual context window and account Memory. Instructions cannot enlarge, clear, or refresh those system limits. What this project can control is the amount of redundant information repeatedly carried into each run.

- Keep **one current compact checkpoint** and a small versioned set of reference files. State: timestamp/timezone, task/run ID and status, main SHA, each open PR's head SHA, current CI/review status, current artifact path/hash, exact error, next action.
- Keep the current checkpoint near 1–2 KB where practical. Link to long logs rather than pasting them repeatedly.
- Read large logs in chunks; filter by error/test/commit/run ID first. Summarize findings and preserve the original log path or run URL for audit.
- Store full run logs only when useful for reproducibility. Keep recent detail; move stale run notes into a dated archive, never delete the only good source or overwrite the sole known-good artifact.
- De-duplicate repeated rules in new prompt revisions. Keep a concise master prompt; preserve previous versions in Archive. Do not place the same 5,000-word checkpoint at the beginning and end of every file.
- Use bounded buffers, pagination and targeted searches. Before packaging, estimate output size and verify each stage.
- Do not claim that `System.gc()`, a “memory refresh”, deleting chat content, or a prompt can reset hidden ChatGPT context/account quotas. If a tool reports a quota limit, stop that execution path, preserve state and switch to an available alternative.

## 5. CodeRabbit staleness prevention

For every open PR:

1. Fetch the current PR head SHA first.
2. Fetch recent Actions status, formal reviews, inline comments and unresolved review threads.
3. A formal review/approval counts as current only if it refers to the current head. A bot “review triggered” comment, email, previous approval or old diff is not sufficient.
4. If a substantive head change makes feedback stale, wait for the event-triggered review or request one explicit review on that PR when supported. Do not add repeated hourly comments to CodeRabbit.
5. Validate findings against the current source/tests. Accepted findings need a minimal code/test fix; false positives should be explained briefly. Run CI after the fix and check that review feedback for the new head is fresh.
6. If the bot does not respond, continue deterministic tests and documentation; record “CodeRabbit review pending/unavailable on SHA …”. Do not claim review success.

Hourly quality workflow verifies the main branch; PR/review events verify the affected PR head. Verify an actual scheduled run separately before claiming the hourly heartbeat has fired.

## 6. Credential and change safety

- No secrets in prompts, source, public comments, or logs. Use only explicitly authorized GitHub/host credentials with least privilege.
- No direct application-code commits to `main`; use a topic branch and PR. Do not auto-merge.
- If upload is throttled, stop uploads for that workflow in the current session. Continue tests/docs/local versioned artifacts and provide verified local links. Only attempt Library upload again after the user explicitly requests a retry or a later session confirms the limit is cleared.
- A request/run can be queued even when it later fails. Never represent accepted, started, passed, uploaded and delivered as the same status.

## 7. Current verified repository layer

- Repository: https://github.com/mrfahimhawladar/calculator-android-app
- `main` merge commit for PR #4: `0a71e15e1678c7daebd8165d4c12d1b0879b062d`.
- Main-push Continuous Quality Loop run `39808885681` and Source CI run `39808885703` passed on that merge.
- PR #2 and #3 were still open at the last verified checkpoint. Re-fetch their heads, base mergeability, CI, review status and threads before any action.
- No separately deployed, independently health-checked, continuously code-writing external agent has been verified. To add one, an authorized host, scoped repository write credential, approved model/API configuration, monitoring, billing/permission approval and a bounded review gate are still required.

## Official references

- Android persistent work / WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Foreground services: https://developer.android.com/develop/background-work/services/fgs/launch
- Android foreground-service changes: https://developer.android.com/develop/background-work/services/fgs/changes
- CodeRabbit is verified through GitHub review records, not an assumed permanent process.
