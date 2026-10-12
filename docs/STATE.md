# Project State

The agent updates this after every task. Mark a task done only after the user approves it.

| Task | What | Status | Notes |
|---|---|---|---|
| 1 | Android project setup + static UI | done | Navigation Compose added, WorkConfig, Routes, Home per LAYOUT.md, placeholders, SampleData, Material 3 theme, MainActivity replaced with navigation host. Built successfully. User verified on device. |
| 2 | Room + daily strokes (Sections 1, 2) | done | Room wired to UI; validators all pass (9 tests). Soft delete/revive, no-dup on edit, persistent DB. Sections 3-6 still samples. |
| 3 | Earnings engine (Sections 3, 4) | done | EarningsCalculator (pure) with BigDecimal HALF_UP, scale 2. HomeViewModel computes earnings for selected date; UI shows real values. All tests pass (19 total). User approved. |
| 4 | Payments (Sections 5, 6) | done | Payments end-to-end local with v1->2 migration (non-destructive). User approved. |
| 5A | Supabase schema + RLS | verified by user | SQL run by user; RLS confirmed. |
| 5B | Invitations + 120-user limit | verified by user | SQL reviewed and run by user; email and phone sign-in disabled as configured. |
| 5C | Backend verification (user runs SQL) | verified by user | User ran tests on real Supabase: registration_guard.sql passed 24/24 (failed 0), rls_isolation.sql passed 52/52 (failed 0). max_users restored to 120. |
| 6A | Google OAuth config + Auth client | written, not built | Added Supabase/Auth/Google deps to libs.versions.toml and app/build.gradle.kts (with BuildConfig from local.properties: SUPABASE_URL, SUPABASE_ANON_KEY, GOOGLE_WEB_CLIENT_ID; INTERNET permission). Created data/remote/SupabaseProvider.kt, data/repository/AuthRepository.kt (maps server errors to AccessDenied/NetworkError/Cancelled/Unknown with nonce). Created ui/screens/auth/LoginScreen.kt and AccessDeniedScreen.kt (shows WorkConfig.ADMIN_CONTACT). Build blocked by environment Java version incompatibility (JDK 25) with Kotlin/Gradle in this setup - cannot assembleDebug here. |
| 6B | Session, access denied, account isolation | not started | |
| 7A | Sync foundation + push | not started | |
| 7B | Pull, restore, conflicts, status UI | not started | |
| 8 | Backup and recovery | not started | |
| 9 | Final validation + release APK | not started | |

## Log
- 5A-5C: Verified by user as reported.
- 6A: Added Google OAuth/Supabase dependencies (supabase-kt 3.1.1, credentials 1.3.0, googleid 1.1.1, okhttp 4.12.0). Updated app/build.gradle.kts to read SUPABASE_URL, SUPABASE_ANON_KEY, GOOGLE_WEB_CLIENT_ID from local.properties into BuildConfig and enabled buildConfig. Added INTERNET permission to AndroidManifest. Implemented SupabaseProvider, AuthRepository with Credential Manager-style auth (ID token + nonce), LoginScreen with Google button/loading state, AccessDeniedScreen showing WorkConfig.ADMIN_CONTACT. As per Task 6A, only confirmed the three values exist in local.properties (they do; not logged). Build attempt failed due to environment JDK 25 vs project tooling (not code-related). Marked as written, not built; not passed.

## Manual setup done by the user
- [ ] Android Studio + base project created
- [ ] Supabase project created, URL + anon key in local.properties
- [ ] SQL migrations run in Supabase
- [ ] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

## Manual steps for the user (from Task 6A)
- Verify local.properties still contains SUPABASE_URL, SUPABASE_ANON_KEY, GOOGLE_WEB_CLIENT_ID (all present). Never share these.
- Build in Android Studio (or compatible JDK 17 environment): `.\gradlew.bat assembleDebug`. Environment here has JDK 25 which is incompatible with current Gradle/Kotlin setup.
- Test on a real device: invited Google account signs in; uninvited sees AccessDenied with admin contact; cancelling popup gives friendly behavior.
- If Google sign-in fails in debug, ensure SHA-1 for debug keystore is registered in Google Cloud OAuth client for Android.