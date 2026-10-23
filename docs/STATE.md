# Project State

The agent updates this after every task. Mark a task done only after the user approves it.

| Task | What | Status | Notes |
|---|---|---|---|
| 1 | Android project setup + static UI | done | Navigation Compose added, WorkConfig, Routes, Home per LAYOUT.md, placeholders, SampleData, Material 3 theme, MainActivity replaced with navigation host. Built successfully. User verified on device. |
| 2 | Room + daily strokes (Sections 1, 2) | done | Room wired to UI; validators all pass (9 tests). Soft delete/revive, no-dup on edit, persistent DB. Sections 3-6 still samples. |
| 3 | Earnings engine (Sections 3, 4) | done | EarningsCalculator (pure) with BigDecimal HALF_UP, scale 2. HomeViewModel computes earnings for selected date; UI shows real values. All tests pass (19 total). User approved. |
| 4 | Payments (Sections 5, 6) | done | Payments end-to-end local with v1->2 migration (non-destructive). User approved. |
| 5A | Supabase schema + RLS | verified by user | SQL run by user; RLS confirmed. |
| 5B | Invitations + 120-user limit | verified by user | SQL reviewed and run by user; email/phone sign-in disabled as configured. |
| 5C | Backend verification (user runs SQL) | verified by user | User ran tests on real Supabase: registration_guard.sql passed 24/24 (failed 0), rls_isolation.sql passed 52/52 (failed 0). max_users restored to 120. |
| 6A | Google OAuth config + Auth client | done | User tested on real phone: invited account signs in with profile in Supabase; uninvited blocked (access denied) with no profile; cancelling popup shows friendly message. Auth with nonce (raw to Supabase, SHA-256 hex to Google), tightened error mapping, logging. |
| 6B | Session, access denied, account isolation | done | All sub-tasks verified by user on phone. |
| 6B-1 | Session restore + encrypted session storage | done (verified by user on phone) | Added EncryptedSessionManager (AES-256-GCM, Android Keystore), updated SupabaseProvider to initialize with it, TheCountApp now checks session at startup with loading indicator, starts at Home if valid session else Login. |
| 6B-2 | Sign-out + account email display | done (verified by user on phone) | Settings: sign-out button with confirmation dialog, calls AuthRepository.signOut(), navigates to Login clearing back stack. Home & Settings show signed-in account email from session. No local data deleted (D10). |
| 6B-3 | CurrentUserProvider refactor | done (verified by user on phone) | New CurrentUserProvider interface + SessionUserProvider (follows Supabase sessionStatus). StrokeRepository, PaymentRepository, HomeViewModel, ViewModelFactory read userId from provider. |
| 6B-4a | Attach logic (data layer) | built and exercised by user on phone as part of 6B-4 | DAO: countLocalStrokes, getLocalStrokeDates, getClashingDates, attachLocalToUser, countLocalPayments, attachLocalPaymentsToUser. Repositories expose them. AttachManager pure class with 4 outcomes. Unit tests added. |
| 6B-4b | Attach prompt wiring | built and exercised by user on phone as part of 6B-4 | On first sign-in with local data, shows dialog "Attach N local entries to this account?" with counts. Yes calls AttachManager.tryAttach(), No leaves rows as "local". Clash shows dates and changes nothing. SharedPreferences remembers prompted account per device. |
| 6B-4c | Account isolation fix | built and exercised by user on phone as part of 6B-4 | HomeViewModel and repositories now follow userId Flow with flatMapLatest. TheCountApp keys HomeViewModel by userId so new user gets fresh instance. Second account starts empty, first intact. |
| 6B-4 | Attach prompt, per-user isolation, account switching | **written and tested by user on phone** | Attach prompt appears on first sign-in with local data. Second account starts empty. First account's data intact. Sign-out → sign-in as different account works without app restart. |
| 6B-5 | Session & profile validity | done (verified by user on phone) | SessionUserProvider: 10s timeout on Refreshing/Restoring → treats as signed out. AuthRepository.checkProfileStatus() queries profiles table; if missing/inactive → navigates to AccessDenied (no local data changed). Offline: ProfileStatus.Error keeps user on Home. |
| 6 | Settings screen fix | done (verified by user on phone) | Fixed broken `AndroidDialog` import → `AlertDialog` in SettingsScreen.kt. Build passes. Verified by user on phone. |
| 7A-1 | Sync push functions + SQL tests | verified by user (40/40 passed) | |
| 7A-2a | SyncPushResult + DTOs + parsePushResponse | done (30 unit tests pass; exercised on phone through 7A-2b) | |
| 7A-2b | Sync push implementation (WorkManager + SyncManager + SyncChip) | **built and exercised by user on phone** | SyncScheduler schedules periodic 15-min push with network constraint + exponential backoff. SyncManager pushes every PENDING_UPSERT/PENDING_DELETE row via the two push functions; Ok → SYNCED with server version, Conflict → CONFLICT, PermanentError → ERROR, network → retry. SyncChip on Home shows Synced / N pending / Error / Offline. "Sync now" button in Settings triggers one-off push. |
| 7B | Pull (Supabase → local Room DB) | PARTIAL: pull/restore built and exercised by user on phone; conflict screen, sign-out wipe and version-cursor paging NOT built | Pull downloads ALL rows each time (no lastPulledVersion cursor, no paging). Open vs prompts.md 7B: item 1 (cursor + mark CONFLICT instead of skipping pending rows), item 4 (conflict screen), item 5 (sign-out wipe, D10), item 7 (pure-logic tests). |
| 8 | Backup and recovery | built; gaps closed by BackupRules refactor + tests | BackupManager (SAF export, OpenDocument import), BackupRules, Settings UI (Export/Import, MERGE or REPLACE, typed REPLACE confirmation, account-mismatch confirmation), docs/BACKUP.md. User's real export checked: checksum valid, 36 strokes (2026-09-02 to 2026-10-14), 0 payments, no duplicate dates. Payment export/import NOT exercised with real data. BackupRulesTest.kt is an EMPTY file, no backup unit tests exist. |
| 9 | Final validation + release APK | not started | Do after the Task 8 gaps and 7B leftovers are closed or knowingly accepted. |
| UI-1 | Settings final UI pass + Hide Currency (Home) + Working Days (Settings) | verified by user | Not in prompts.md (extra work). |

## Log
- 5A-5C: Verified by user.
- 6A: Implemented and verified on real device; marked done. Updated D13 to note nonce hash detail.
- 6A wiring/error mapping adjustments as needed.
- UI fix: "Add payment" moved from card 6 (Outstanding Balance) to a button on card 5 (Payments Received). Card 6 is no longer clickable. Verified by user.
- UI theme: minimal light-navy and light-orange palette, flat outlined cards, light and dark following the system. Theme files in ui/theme and card styling in ui/screens. Verified by user on phone.
- UI text: removed number prefixes from the six Home card titles; removed "Still owed" from card 6. "Fully settled" and "Overpaid / credit" are kept. Verified by user.
- Build note: Gradle in the agent terminal fails on Java version. Use JAVA_HOME="C:\Program Files\Android\Android Studio\jbr" or build in Android Studio.
- 7A-1: Verified by user (40/40 passed).
- 7A-2 first attempt undone by user (git restore + git clean); restarting in small tasks.
- 7A-2a: SyncPushResult + DTOs + parsePushResponse written, 30 unit tests passed. kotlinx.serialization plugin added (gradle/libs.versions.toml, root build.gradle.kts, app/build.gradle.kts) plus kotlinx-serialization-core dependency. JDK 17 found in Gradle cache and written into SYNC_RULES section C.
- 6: SettingsScreen compile error fixed — broken `AndroidDialog` import replaced with `AlertDialog`. Build passes. Verified by user on phone.
- 7A-2b: Full sync push implemented. Files: data/sync/SyncState.kt (sealed SyncState: Synced/Pending/Error/Offline), data/sync/SyncRepository.kt (exposes pending count + sync state over Room Flow), data/sync/SyncScheduler.kt (WorkManager: periodic 15-min push with network constraint + exponential backoff; syncNow() one-off via OneTimeWorkRequest), data/sync/SyncWorker.kt (CoroutineWorker; reads userId directly from SupabaseProvider.client.auth to avoid SessionUserProvider race), data/sync/SyncManager.kt (pushes every PENDING_UPSERT/PENDING_DELETE stroke + payment row; Ok → markSynced with server version, Conflict → markConflict, PermanentError/Malformed → markError, network → rethrown for retry). UI: SyncChip on Home top row, "Sync now" button in Settings. Build passes; 54 pending rows pushed to Supabase; data visible in Supabase Table Editor (daily_strokes + payments). Verified by user on phone.
- 7B: Pull implemented. Files: data/remote/dto/PullDtos.kt (StrokeRowDto, PaymentRowDto matching server column names), data/sync/Pullers.kt (StrokePuller, PaymentPuller using postgrest.from("table").select(), RLS handles user filtering), data/sync/PullManager.kt (merge: new rows inserted as SYNCED, PENDING rows skipped so local wins, server version newer → update, server deleted → soft-delete). SyncWorker updated to run push then pull in sequence. Sync runs every 15 min automatically or on demand via "Sync now". Verified by user on phone.
- Sync architecture summary: local write → Room (PENDING_UPSERT/PENDING_DELETE) → WorkManager (network-constrained, exponential backoff) → SyncWorker → SyncManager → StrokePusher/PaymentPusher (Supabase RPC thecount_push_stroke / thecount_push_payment) → parsePushResponse → mark SYNCED/CONFLICT/ERROR → SyncManager → PullManager → Pullers (postgrest select) → merge into Room. One global version sequence on the server; strokes and payments have separate pull cursors. Rows owned by "local"/"anonymous" never pushed. Delete vs upsert decided by deletedAt != null, not sync_state. Money is BigDecimal, 2 decimals, HALF_UP, never Double/Float. No tokens, emails or payloads in logs.
- Sync fix (commit 0238bfa): Pullers.kt, SyncScheduler.kt, TheCountApp.kt changed. User confirmed sync works.
- Task 8 (commit b38f1d8): BackupManager.kt, BackupRules.kt, Daos.kt, PaymentDao.kt, SettingsScreen.kt, docs/BACKUP.md, BackupRulesTest.kt (empty). Imported rows become PENDING_UPSERT and sync normally. REPLACE writes a safety copy to app cache.
- Current app status: sync, backup export and import work on device. Remaining: close Task 8 and 7B gaps, then Task 9.
- Unit tests counted by @Test (not re-run): PushResponseParser 30, Validators 9, EarningsCalculator 9, MoneyMask 6, AttachManager 4, ExampleUnitTest 1, BackupRules 0.

## Known issues
Resolved (remove): Home "Synced" label, dead AccessDeniedScreen.kt, stray file "tatus", "data only on this phone".
Task 8 gaps:
(1) MERGE logic looks inverted: BackupRules.shouldApplyImportedRecord applies in MERGE only when the existing row is NOT deleted, so MERGE overwrites active entries and does not restore soft-deleted ones. prompts.md says MERGE adds missing and never deletes.
(2) Import does not check dates >= 2026-09-02 (and not in the future).
(3) Payment amount check accepts >= 0 and any decimals; spec is > 0 and max 2 decimals.
(4) Bad files can show raw JSON/format exceptions instead of a friendly message.
(5) No backup unit tests (BackupRulesTest.kt is 0 bytes); validation is inside BackupManager and not testable.
(6) docs/BACKUP.md claims rules the code does not enforce.
Task 7B gaps: no version cursor/paging; pending rows skipped instead of CONFLICT; no conflict screen; sign-out wipe (D10) not implemented; no pull/merge tests.
Other: no unit tests for payment validators; UI-1 not verified by user.

## Manual setup done by the user
- [x] Android Studio + base project created
- [x] Supabase project created, URL + anon key in local.properties
- [x] SQL migrations run in Supabase
- [x] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

## Manual steps for the user (from Task 6A)
- If Google sign-in fails in release, ensure release SHA-1 is registered in Google Cloud OAuth client for Android.




