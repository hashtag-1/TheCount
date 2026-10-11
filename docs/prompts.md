# TheCount: Task Prompts

AGENTS.md is loaded automatically, so these prompts stay short. Run ONE task at a time and approve it before the next. After each task, check the "You check" list yourself, because the agent cannot see your screen.

| Task | Builds | Depends on | Feeds |
|---|---|---|---|
| 1 | Project, navigation, static six-section home | base project from START-HERE | everything |
| 2 | Room, daily strokes, Sections 1 and 2 | 1 | 3, 7 |
| 3 | EarningsCalculator, Sections 3 and 4 | 2 | 4, 9 |
| 4 | Payments, Sections 5 and 6 | 3 | 7 |
| 5A | Supabase tables, constraints, RLS | none (SQL only) | 5B |
| 5B | Invitations and the 120-user limit | 5A | 5C, 6 |
| 5C | Backend verification, run by YOU | 5B | 6A |
| 6A | Google OAuth config and Auth client | 5C | 6B |
| 6B | Session, access denied, account isolation | 6A | 7 |
| 7A | Sync foundation and push | 4, 6B | 7B |
| 7B | Pull, restore, conflicts, sync status | 7A | 8 |
| 8 | Export/import backup | 7B | 9 |
| 9 | Final validation, release APK, README | all | done |

Model tip: use a fast model for Tasks 1 to 4 and 8. Use a stronger model for 5A, 5B, 6A, 6B, 7A, 7B and 9 (security and sync).

---

## TASK 1: Project setup and static UI

```text
TASK 1: Android project setup and static UI. Do ONLY this task. No Room, Supabase, Google login or sync.

0. Pre-check: look for settings.gradle.kts (or settings.gradle) at the project root. If it is missing, STOP and tell me to create the base project in Android Studio (START-HERE.md step 3). Do NOT hand-write the Gradle wrapper or generate a project from scratch.
1. Inspect the existing project and preserve it. Keep its package name (see docs/DECISIONS.md D1).
2. Configure: app name "TheCount" (strings.xml), Kotlin, Jetpack Compose, Material 3, Navigation Compose, minSdk 26. Add only the libraries this task needs.
3. Create only the packages and files this task needs from docs/ARCHITECTURE.md section 2.
4. Create domain/WorkConfig.kt with constants only: START_DATE = LocalDate.of(2026, 9, 2), RATE_PER_STROKE = BigDecimal("0.04"), TAX_RATE = BigDecimal("0.01"), CURRENCY = "रु", MAX_USERS = 120, ADMIN_CONTACT = "" (empty for now). No calculation code.
5. Navigation Compose routes: home, entry, calendar, earnings, payments, addPayment, settings, login, accessDenied. Start destination: home. Settings has a temporary link to login.
6. Home screen exactly as in docs/ui/LAYOUT.md: top bar, status row (date, account chip, sync chip, with static text), then the six numbered cards in order. Cards 2, 4, 5 and 6 are clickable and open their screens. If image files exist in docs/ui/, view them and follow them over LAYOUT.md.
7. Every other screen is a placeholder: title, back button, short text "Coming in Task N".
8. Put all sample values in ONE object SampleData, clearly named, to be removed later. Money here is sample text only (no calculation).
9. Theme: Material 3, restrained neutral palette, follows system light/dark. Check that "रु" renders correctly.
10. Build with .\gradlew.bat assembleDebug. If the Android SDK or JDK is missing, say exactly what is missing and stop.
11. Update docs/STATE.md, report, and STOP.
```

**You check:** the app opens on a phone or emulator; six numbered cards appear in order; tapping cards 2, 4, 5, 6 opens placeholder screens and Back works; "रु" displays properly; dark mode is readable.

---

## TASK 2: Room and daily strokes (Sections 1 and 2)

```text
TASK 2: Room database and daily stroke history. Do ONLY this task. No money calculation (Sections 3 to 6 keep their sample values).

1. Add Room and KSP with the minimum dependencies. Follow docs/ARCHITECTURE.md section 5 exactly for the daily_strokes table, INCLUDING the sync columns (syncState, serverVersion, deletedAt) even though sync is later. userId is "local" for now (docs/DECISIONS.md D4).
2. Create Entities.kt, Daos.kt, TheCountDatabase.kt, Converters.kt (dates stored as ISO text) and StrokeRepository.kt. Soft delete only. If a row for (userId, workDate) exists, even soft-deleted, UPDATE or REVIVE it instead of inserting a second one (D6).
3. Create domain/Validators.kt (pure Kotlin): the count must be a non-negative whole number. Reject blank, negative, decimal, non-numeric, and numbers that overflow Long. The date must be on or after 2026-09-02 and not after today (D2).
4. Section 1 (home) and DailyEntryScreen: today's date, numeric input, Save or Update, the saved count for today, edit allowed. Show validation errors in plain words.
5. Section 2 and CalendarScreen: history from 2026-09-02 up to today. Dates with an entry show the count. Dates without an entry look different and are NEVER treated as zero. Tapping a date opens it for editing. Add a delete action with confirmation.
6. Create the shared selectedDate state in HomeViewModel as described in docs/ARCHITECTURE.md section 4. Section 2 shows the selected date's saved count or "No entry".
7. Unit tests for Validators (boundaries: 0, 1, -1, "1.5", "abc", blank, start date, day before start date, tomorrow). Run .\gradlew.bat testDebugUnitTest.
8. Do not add Supabase, login or sync code. Update docs/STATE.md, report, and STOP.
```

**You check:** enter today's count, close the app completely, reopen it, and the number is still there; edit an older date and it changes; Sept 1 is not selectable; a count of -5 or 2.5 is rejected; a date with no entry does not show "0".
**Tip:** your real strokes since 2 Sep can be entered now. They stay on the phone and carry over at login (D4).

---

## TASK 3: Earnings engine (Sections 3 and 4)

```text
TASK 3: Central earnings calculation. Do ONLY this task. Payments do not exist yet, so payments total = 0.

1. Create domain/EarningsResult.kt and domain/EarningsCalculator.kt, pure Kotlin, no Android imports. Use BigDecimal only, never Double or Float. Follow the formulas in docs/ARCHITECTURE.md section 6 exactly:
   Gross = strokes x WorkConfig.RATE_PER_STROKE; Tax = (Gross x TAX_RATE) rounded HALF_UP to scale 2; Net = Gross - Tax; Outstanding = Net - payments (never clamped).
   Signature idea: calculate(strokes: Long, payments: BigDecimal): EarningsResult.
2. Add a DAO query for the cumulative stroke sum: workDate from 2026-09-02 through the selected date, deletedAt null, current userId. Expose it as a Flow from StrokeRepository.
3. HomeViewModel combines the selected date, the sum flow and the calculator, and exposes results. The UI never does money math.
4. Section 3 shows cumulative strokes with digit grouping (100,000). Section 4 shows the receivable amount; tapping opens EarningsScreen, which shows the selected date and exactly: 4.1 Total Earning, 4.2 Tax = 1%, 4.3 Receivable Income. Read-only. Format with util/MoneyFormat.kt: "रु 4,000.00".
5. Remove the sample values for Sections 3 and 4 (keep 5 and 6 sample until Task 4).
6. Unit tests (JUnit): 0 strokes; 1 stroke; 100,000 strokes -> 4,000.00 / 40.00 / 3,960.00, with payments 2,000.00 -> outstanding 1,960.00; 12 strokes -> gross 0.48, tax 0.00; 13 strokes -> gross 0.52, tax 0.01, net 0.51; a very large count (for example 9,000,000,000); multiple payments; payments greater than net -> negative outstanding; an "edited historical entry" test (recalculate after the sum changes). Run .\gradlew.bat testDebugUnitTest and report real counts.
7. Update docs/STATE.md, report, and STOP.
```

**You check:** the unit tests pass; entering strokes updates Sections 3 and 4 at once; changing the selected date to an earlier day lowers the totals; editing an old day updates today's cumulative; the breakdown shows the date and three values.

---

## TASK 4: Payments (Sections 5 and 6)

```text
TASK 4: Payment tracking. Do ONLY this task.

1. Add the payments table per docs/ARCHITECTURE.md section 5 (amount stored as TEXT through a BigDecimal converter, D5), with sync columns, userId "local", soft delete. Bump the Room version and add a proper migration. Do NOT use destructive migration, because I have real data.
2. PaymentRepository: observe all (newest first), observe the total of payments with paymentDate <= selected date, add, edit, soft delete.
3. Validators: amount must be greater than 0 with at most 2 decimals, parsed with BigDecimal (reject blank, "abc", "1.234", negative, zero). Date on or after 2026-09-02 and not in the future (D3). Note is optional.
4. AddPayment screen or dialog: amount, date received, optional note, Save and Cancel. Multiple payments on the same date are allowed.
5. PaymentsScreen: history newest first. Edit and delete (with confirmation). Section 5 shows total received and a "+ Add payment" button.
6. Section 6: connect to the calculator with real payments. Positive = still owed. Zero = "Fully settled". Negative = "Overpaid / credit" with a clearly different style. Never clamp it to zero.
7. Do not deduct tax again from payments (payments are the net amounts received).
8. Remove the remaining sample values and SampleData if nothing uses it.
9. Unit tests for the payment validators. Run .\gradlew.bat testDebugUnitTest.
10. Update docs/STATE.md, report, and STOP.
```

**You check:** BEFORE this task, write down your cumulative stroke total. Add two payments on the same date; edit one; delete one (confirmation appears); Section 6 updates each time; add a payment bigger than the receivable and see "Overpaid"; changing the selected date to before a payment excludes it; restart the app and nothing is lost; your earlier stroke data is intact after the migration.

---

## TASK 5A: Supabase schema and RLS (SQL only)

```text
TASK 5A: Supabase database schema and Row Level Security. Do ONLY this task. Write SQL files, no Android code.
IMPORTANT: you cannot run SQL against my Supabase project (you have no credentials and must never ask for the service-role key). Write the SQL and tell me exactly how to run it. Do not claim anything is tested.

1. Create numbered files in supabase/migrations/: 001_schema.sql, 002_triggers.sql, 003_rls.sql. Each file must be safe to read top to bottom and run once in the Supabase SQL editor.
2. Tables per docs/ARCHITECTURE.md section 5: profiles, daily_strokes, payments (invitations, admins and app_config come in 5B; you may create empty shells now).
   - Constraints: daily_strokes UNIQUE(user_id, work_date), stroke_count >= 0, work_date >= '2026-09-02'; payments amount numeric(14,2) CHECK (amount > 0); payment_date >= '2026-09-02'.
   - Foreign keys to auth.users with ON DELETE CASCADE where appropriate. Indexes on (user_id, work_date), (user_id, payment_date), and on (user_id, version) for sync pulls.
3. Triggers (002): on every insert and update set updated_at = now() and bump version on the SERVER, so clients cannot forge them. user_id can never be changed by an update.
4. RLS (003): enable RLS on EVERY table. Users read their own profile only. Users select, insert, update and delete only rows where user_id = (select auth.uid()) AND they have an active profile (use a small SECURITY DEFINER helper function with a fixed search_path). No policies at all for invitations, admins and app_config for ordinary users.
5. Soft delete: clients UPDATE deleted_at; there is no hard-delete path for ordinary users unless the task needs it.
6. Write supabase/README.md: how to run the three files in order in the SQL editor, and how to confirm in the Table Editor that RLS is ON for each table.
7. Update docs/STATE.md (mark "SQL written, NOT yet run by user"), report, and STOP.
```

**You do:** run the three files in the Supabase SQL editor in order; confirm each table shows RLS enabled.

---

## TASK 5B: Invitations and the 120-user limit

```text
TASK 5B: Invitation-only registration and the 120-account limit, enforced on the SERVER. Do ONLY this task. Same rule as 5A: you cannot run SQL against my project, so write it and give me exact run steps. Do not claim the limit works until I report test results (that is Task 5C).

1. You may use web search for the official Supabase docs for this task only. Evaluate, in this order, and record the choice with reasons in docs/DECISIONS.md: (a) Auth Hook "Before User Created", (b) a trigger on auth.users, (c) an Edge Function flow. Prefer the option that REJECTS the sign-up before a usable account exists. Check what Google sign-in through Supabase actually does in the chosen design.
2. Create 004_invitations.sql: tables invitations (email stored lowercase, status active/used/revoked, created_at, expires_at NULL allowed, used_at, used_by, optional id), admins (managed only in the dashboard, no client policies), app_config(key, value) with max_users = 120 (docs/DECISIONS.md D8). RLS on all, no ordinary-user access. A unique partial index so an email has at most one active invitation.
3. Create 005_registration_guard.sql with the server-side check, in ONE locked transaction (pg_advisory_xact_lock or equivalent): the Google email must match an active, unexpired invitation; the number of registered accounts must be below max_users; the slot is RESERVED in the same transaction (for example by marking the invitation used and counting used invitations plus existing profiles) so two concurrent sign-ups cannot both take the last slot. On success create the profile with status active. On failure raise a clear error message the app can recognise.
4. Explain, in supabase/README.md, what happens to a provisional account if anything fails after the check, and a safe cleanup query. Explain how the profile and RLS stay unusable until the invitation check passes.
5. Admin guide (supabase/README.md): exact SQL to invite an email, list invitations, revoke one, change max_users, and see the current user count. An invitation can never be reused by another email or create a duplicate account.
6. Do not put any privileged key anywhere in the repo. Update docs/STATE.md (SQL written, NOT yet run or tested), report, and STOP.
```

---

## TASK 5C: Backend verification (YOU run it, the agent writes the scripts)

```text
TASK 5C: Write the backend verification scripts. Do ONLY this task. You cannot run them against my Supabase project; I will run them and report the results. Do not mark anything as passed.

1. supabase/tests/rls_isolation.sql: runnable in the Supabase SQL editor. It creates two test users, then impersonates each (set local role authenticated and set_config('request.jwt.claims', ...)) and proves: user A cannot select, insert, update or delete user B's daily_strokes, payments or profile, even by guessing a UUID; a user cannot change user_id; a user cannot read or write invitations, admins or app_config; an anonymous role sees nothing. Each check prints PASS or FAIL with a description. The script cleans up its test data at the end.
2. supabase/tests/registration_guard.sql: tests with max_users temporarily set to 3. Cases: uninvited email is rejected; expired invitation rejected; revoked invitation rejected; valid invitation accepted once; the same invitation cannot create a second account; the 4th account is rejected. Restores max_users to 120 at the end and says so.
3. supabase/tests/concurrency/: a small separate Node project (its own package.json using the pg package, NOT part of the Android build) with run.mjs. It reads DATABASE_URL from an environment variable (never from a file in the repo), sets max_users to 3 with 10 active invitations, fires 10 concurrent registrations through the same server function the guard uses, and checks that exactly 3 succeed. It restores max_users and cleans up afterwards.
4. supabase/tests/README.md: exact steps for me to run each test, where to find the connection string in the Supabase dashboard, how to keep it out of the repo, and the expected output.
5. Update docs/STATE.md ("scripts written; waiting for the user to run them"), report, and STOP.
```

**You do:** run the three tests and paste the real results into the chat. Only then does the agent mark 5A to 5C as verified. Do not skip this, it is the proof that the 120 limit and the data isolation work.

---

## TASK 6A: Google OAuth config and Auth client

```text
TASK 6A: Google sign-in through Supabase. Do ONLY this task. Do not build sync.

0. Pre-check: docs/STATE.md must show Task 5C verified by me. Check that local.properties has SUPABASE_URL, SUPABASE_ANON_KEY and GOOGLE_WEB_CLIENT_ID. If any is missing or blank, STOP and point me to docs/SETUP-MANUAL.md. Never invent values.
1. You may use web search for the official docs for this task only: the current supabase-kt library and the current native Google sign-in flow (Credential Manager with a Google ID token passed to Supabase). Do not use deprecated sign-in APIs. Record the library versions and the flow chosen in docs/DECISIONS.md.
2. Read the three values from local.properties into BuildConfig fields (in app/build.gradle.kts). Make sure local.properties is gitignored. The service-role key and OAuth client secret must NEVER appear in the app.
3. Add the minimum dependencies, the INTERNET permission, data/remote/SupabaseProvider.kt and data/repository/AuthRepository.kt with: signInWithGoogle(), currentSession(), signOut(). Map failures to a small sealed result: Success, Cancelled, NetworkError, AccessDenied (the server rejection from Task 5B), Unknown.
4. LoginScreen: "Sign in with Google" button, loading state, readable error messages (cancelled, offline, access denied). AccessDeniedScreen: explains the account is not invited and shows WorkConfig.ADMIN_CONTACT.
5. Do not implement session restore, sign-out wiping or data attachment yet (Task 6B).
6. Build with .\gradlew.bat assembleDebug. Update docs/STATE.md, list the manual steps I must still do, report, and STOP.
```

**You check (on a real phone):** an invited Google account signs in and a profile exists in Supabase; an uninvited account sees the access-denied screen and no profile exists; cancelling the Google popup shows a friendly message.

---

## TASK 6B: Session, access denied, account isolation

```text
TASK 6B: Session handling and account isolation. Do ONLY this task.

1. Session: restore the session at app start. Start destination = home if signed in, otherwise login. Keep tokens only in the library's supported secure session storage; if its default is plain SharedPreferences, replace it with an encrypted, Keystore-backed store. Say what you chose and why in docs/DECISIONS.md.
2. Sign-out with confirmation, then go to login. In this task sign-out must NOT delete any local data, because nothing is in the cloud yet (sync arrives in Task 7). Rows stay tagged with their userId and every query filters by the signed-in user, so nothing is exposed. The wipe-on-sign-out rule (D10) is added in Task 7B.
3. Account isolation: every repository query filters by the signed-in user's id. Replace the temporary userId "local" with the real id as in D4: on the FIRST sign-in, if "local" records exist, ask once "Attach N local entries to this account?" (default Yes, show the counts). Never attach local records to a second account. A brand-new Google account with no local data starts empty.
4. Show the account email in the status row and in Settings. If a session becomes invalid or the profile is not active, send the user to login or access denied.
5. Do not implement sync yet. Records are still only on this phone.
6. Update docs/STATE.md, list manual checks, report, and STOP.
```

**You check:** BEFORE this task, write down your cumulative stroke total and payment total. After first login and the attach prompt, the same numbers appear. Close and reopen the app and you stay signed in. Sign out and sign in again, and your data is still there. Sign in with a second invited account and it starts empty, never showing the first account's records.

---

## TASK 7A: Sync foundation and push

```text
TASK 7A: Offline synchronization, part 1 (foundation and push). Do ONLY this task.

1. Follow docs/ARCHITECTURE.md section 8 and docs/DECISIONS.md D9. Add WorkManager. Create data/remote DTOs and data/sync/SyncManager.kt, SyncWorker.kt.
2. Triggers: after any local write, at app start, and when the network returns (unique work with a network constraint and exponential backoff). Writes always go to Room first; the UI never waits for the network.
3. Push every row whose syncState is PENDING_UPSERT or PENDING_DELETE: strokes and payments, new, edited and soft-deleted. Writes are idempotent (same UUID can be sent twice safely). For strokes use the (user_id, work_date) uniqueness correctly: if the server already has that date under a different id, do not create a duplicate; mark the local row CONFLICT instead of overwriting.
4. Send the base serverVersion with each edit. The server must reject an edit when its version differs (optimistic concurrency); implement this with a small RPC or a filtered update, and add the SQL as supabase/migrations/006_sync.sql. Tell me exactly how to run it. On success store the server's new version and set SYNCED. Never show "backed up" before the server confirms.
5. Failures: network errors keep the row pending with retry; permanent errors set ERROR with a readable reason.
6. SyncChip on Home: Synced, N pending, Error, Offline. Settings gets "Sync now".
7. Do not implement pull or restore yet. Unit-test the pure parts (state transitions, backoff, conflict detection). Update docs/STATE.md, list manual steps, report, and STOP.
```

**You check:** turn on airplane mode, add a stroke and a payment, and the chip shows pending; turn it off and it goes to Synced; the rows appear in the Supabase Table Editor under your user only; the same entry is never duplicated.

---

## TASK 7B: Pull, restore, conflicts, status

```text
TASK 7B: Offline synchronization, part 2 (pull, restore, conflicts, sign-out wipe). Do ONLY this task.

1. Pull: fetch rows for the signed-in user with version greater than sync_meta.lastPulledVersion, in pages. Apply them. Never overwrite a row that is still pending locally; compare versions and mark CONFLICT instead (docs/DECISIONS.md D9).
2. Deletions: pulled soft-deleted rows delete (soft) the local row, so deleted records never reappear on another device.
3. Initial restore / new phone: an empty local database means "download everything", NEVER "delete cloud records". Download only the signed-in account's data, then everything recalculates from restored data. A new Google account starts empty. Do not merge unrelated local data automatically.
4. Conflict screen or dialog: shows the local and server values side by side and lets me keep one. No financial record is silently discarded.
5. Implement the sign-out wipe from docs/DECISIONS.md D10: wipe local data only when nothing is pending; if something is pending, warn and offer "Sync now" instead.
6. Make sure one account can never receive another account's rows (filter by userId in every query, and rely on RLS as the second layer).
7. Unit-test the pure logic (cursor handling, conflict marking, empty-local-database rule). Update docs/STATE.md, list manual steps, report, and STOP.
```

**You check (needs two devices or a reinstall):** sign in on a second phone or after reinstalling, and all strokes and payments appear with the same totals; edit the same payment on two devices offline and the conflict screen appears; delete on one device and it disappears from the other; a different Google account sees none of it.

---

## TASK 8: Backup and recovery

```text
TASK 8: Export and import backup. Do ONLY this task.

1. Export: write one JSON file through the Storage Access Framework (user picks the location). Include app name, format version, exportedAt, the account id, all non-deleted strokes and payments (money as decimal strings), and a checksum.
2. Import: pick a file, then validate before touching any data: correct app name and version, valid checksum, every date on or after 2026-09-02, no negative strokes, payment amounts greater than 0 with 2 decimals, no duplicate stroke dates. Show a summary (counts, date range) first.
3. Ownership: imported rows always become the signed-in user's rows. If the file belongs to a different account, show a clear warning and require explicit confirmation.
4. Modes: MERGE (default: match strokes by date and payments by id, add missing, never delete) and REPLACE (requires a typed confirmation and first creates an automatic export of the current data). State clearly which mode is running. Prevent accidental duplication.
5. Imported rows go through the normal sync path (they become pending).
6. Add docs/BACKUP.md: how cloud sync differs from a real backup, and the limits of my Supabase plan. Use web search for the CURRENT Supabase plan limits (backups, project pausing) and cite the pages you used.
7. Unit-test the validator and the merge logic. Update docs/STATE.md, report, and STOP.
```

**You check:** export, delete a few entries, import in MERGE mode and they come back without duplicates; import a damaged file and it is rejected with a clear message; REPLACE asks for typed confirmation; reinstall the app, sign in, and everything restores.

---

## TASK 9: Final validation and release APK

```text
TASK 9: Final validation and release APK. Do ONLY this task. Never report an untested item as passed.

1. Run .\gradlew.bat testDebugUnitTest and report real counts. Add missing tests for: calculations, date boundaries (2026-09-01, 2026-09-02, today, tomorrow), historical edits, payment history, sync state transitions, import validation.
2. Create docs/TEST-REPORT.md with a table of every checklist item below, with status PASSED (tested, how), FAILED, or NOT TESTED (and why): calculations; date boundaries; historical edits; payments; login and invitation restriction; the 120 limit and concurrent registration (from my 5C results); RLS isolation (from my 5C results); offline use and reconnect; new-device restore; fresh install; backup and restore.
3. Release build: add signing config that reads keystore.properties (gitignored; never commit the keystore or passwords). Per docs/DECISIONS.md D12 keep minify off unless fully tested. Build with .\gradlew.bat assembleRelease and report the APK path. If signing is not set up, STOP and point me to docs/SETUP-MANUAL.md section 8.
4. Remember the release SHA-1 must be registered as an Android OAuth client in Google Cloud, otherwise Google sign-in fails in the release APK. Remind me.
5. Final scan: no secrets in the repo, no Double/Float in money code, no leftover SampleData.
6. Write README.md: what the app does, how to install the APK, how to invite users, how to back up, known limitations. Update docs/STATE.md, report, and STOP.
```

---

## Recovery prompts

**Task not finished properly**
```text
This task is not complete. Re-test it against its checklist in docs/prompts.md, list exactly what failed, fix only those items, and report again.
```

**Agent added extras**
```text
You changed or added things outside the current task (<name them>). Revert those and stay within the task scope and AGENTS.md.
```

**Build is failing**
```text
Run .\gradlew.bat assembleDebug, copy the FIRST error exactly, fix only the cause of that error, and rebuild. Stop after 3 attempts and show me the error.
```

**Write tool keeps failing**
```text
Your write tool calls are failing. Write ONE small file per call with filePath and full content. Split large files into several smaller files or append in parts. If it still fails after one retry, STOP and tell me which file you could not write.
```
