# Sync rules (standing rules for Tasks 7A-2 to 7B). Read before every task.

## A. Facts
- 7A-1 is done: 006_sync.sql and sync_push.sql ran on the user's Supabase, 40/40 passed. Never edit SQL.
- Server push functions (already live):
  thecount_push_stroke(p_id uuid, p_work_date date, p_stroke_count bigint, p_deleted boolean, p_base_version bigint) returns jsonb
  thecount_push_payment(p_id uuid, p_payment_date date, p_amount numeric, p_note text, p_deleted boolean, p_base_version bigint) returns jsonb
- The user is always auth.uid() on the server. Never send a user id to these functions.
- Result: {"status":"ok","version":N} or {"status":"conflict","reason":"version_mismatch|date_exists_other_id|insert_blocked","version":N, plus server values}.
  Stroke conflict extras: server_id, server_stroke_count, server_deleted.
  Payment conflict extras: server_payment_date, server_amount (text), server_note, server_deleted.
- Bad data (date before 2026-09-02, negative strokes, amount <= 0) and permission problems raise Postgres errors: these are PERMANENT errors, not network errors.
- The server assigns `version` (one global increasing sequence shared by both tables). The client sends its stored server_version as p_base_version (null if never synced). Retrying an identical push returns ok.
- sync_state is a String column: PENDING_UPSERT, PENDING_DELETE, SYNCED, CONFLICT, ERROR. Use existing constants if they exist. Never change stored values.
- Because version is global across tables, strokes and payments need SEPARATE pull cursors.
- Rows owned by "local" or "anonymous" are never pushed. Delete vs upsert is decided by deletedAt != null, not by sync_state.

## B. Rules
- No Room schema change or migration. If you think one is needed, STOP and ask.
- No new dependency unless the task names it. Do not upgrade AGP, Kotlin, Gradle or compileSdk. No Hilt/Koin: obtain the database and Supabase client the way the app already does.
- Never delete or wipe local data unless the task says so. Pull code never writes to the server.
- Never log tokens, emails or payloads. Logs contain only counts and short error classes.
- Money is BigDecimal, 2 decimals, HALF_UP. Never Double or Float.
- Pure logic lives in plain Kotlin classes with no Android imports so unit tests run without a device. Do not add Robolectric or androidTest.
- Keep all existing imports when editing. Every symbol you use must be imported. One file per write call. Rethrow CancellationException, never swallow it.
- Touch only the files the task lists. If you need another file, STOP and ask.
- Do not start the next task. Never mark a task "done": only the user does.

## C. FINISH STEPS (every task ends with these)
1. Build and test: .\gradlew.bat assembleDebug testDebugUnitTest "-Dorg.gradle.java.home=C:\Users\Sandesh Bhandari\.gradle\jdks\eclipse_adoptium-17-amd64-windows.2"
   (First time: find the JDK 17 in the Gradle JDK cache and write the exact command here, replacing the placeholder. Never edit gradle.properties. Android Studio's JBR is JDK 25 and AGP 8.9.1 rejects it.)
2. If it fails: copy the FIRST error, fix only its cause, max 3 attempts, then STOP and show me the error.
3. Run git status and git diff --stat. Only the task's files may change. gradle.properties must NOT appear. Revert anything else.
4. Update the task's row in docs/STATE.md: "written, N tests passed, not verified by user".
5. Report: files changed, the names of the tests you added, real pass counts, anything you assumed or skipped. Then STOP.