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
| 6B | Session, access denied, account isolation | in progress: 6B-1 to 6B-4 done, 6B-5 awaiting user test | 6B-1 to 6B-4 built and exercised by user on phone. 6B-5 written, awaiting user test. Build/test not run from terminal (Java 25 breaks Gradle 8.11.1). |
| 6B-1 | Session restore + encrypted session storage | built and exercised by user on phone as part of 6B-4 | Added EncryptedSessionStorage (AES-256-GCM, Android Keystore), updated SupabaseProvider to initialize with it, TheCountApp now checks session at startup with loading indicator, starts at Home if valid session else Login. |
| 6B-2 | Sign-out + account email display | built and exercised by user on phone as part of 6B-4 | Settings: sign-out button with confirmation dialog, calls AuthRepository.signOut(), navigates to Login clearing back stack. Home & Settings show signed-in account email from session. No local data deleted (D10). |
| 6B-3 | CurrentUserProvider refactor | built and exercised by user on phone as part of 6B-4 | New CurrentUserProvider interface + SessionUserProvider (follows Supabase sessionStatus). StrokeRepository, PaymentRepository, HomeViewModel, ViewModelFactory read userId from provider. |
| 6B-4a | Attach logic (data layer) | built and exercised by user on phone as part of 6B-4 | DAO: countLocalStrokes, getLocalStrokeDates, getClashingDates, attachLocalToUser, countLocalPayments, attachLocalPaymentsToUser. Repositories expose them. AttachManager pure class with 4 outcomes. Unit tests added. |
| 6B-4b | Attach prompt wiring | built and exercised by user on phone as part of 6B-4 | On first sign-in with local data, shows dialog "Attach N local entries to this account?" with counts. Yes calls AttachManager.tryAttach(), No leaves rows as "local". Clash shows dates and changes nothing. SharedPreferences remembers prompted account per device. |
| 6B-4c | Account isolation fix | built and exercised by user on phone as part of 6B-4 | HomeViewModel and repositories now follow userId Flow with flatMapLatest. TheCountApp keys HomeViewModel by userId so new user gets fresh instance. Second account starts empty, first intact. |
| 6B-4 | Attach prompt, per-user isolation, account switching | **written and tested by user on phone** | Attach prompt appears on first sign-in with local data. Second account starts empty. First account's data intact. Sign-out → sign-in as different account works without app restart. |
| 6B-5 | Session & profile validity | written, awaiting user test | SessionUserProvider: 10s timeout on Refreshing/Restoring → treats as signed out. AuthRepository.checkProfileStatus() queries profiles table; if missing/inactive → navigates to AccessDenied (no local data changed). Offline: ProfileStatus.Error keeps user on Home. Build/test not run from terminal (Java 25 breaks Gradle). |
| 7A | Sync foundation + push | not started | |
| 7B | Pull, restore, conflicts, status UI | not started | |
| 8 | Backup and recovery | not started | |
| 9 | Final validation + release APK | not started | |

## Log
- 5A-5C: Verified by user.
- 6A: Implemented and verified on real device; marked done. Updated D13 to note nonce hash detail.
- 6A wiring/error mapping adjustments as needed.

## Manual setup done by the user
- [ ] Android Studio + base project created
- [ ] Supabase project created, URL + anon key in local.properties
- [ ] SQL migrations run in Supabase
- [ ] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

## Manual steps for the user (from Task 6A)
- If Google sign-in fails in release, ensure release SHA-1 is registered in Google Cloud OAuth client for Android.