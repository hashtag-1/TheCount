# Project State

The agent updates this after every task. Mark a task done only after the user approves it.

| Task | What | Status | Notes |
|---|---|---|---|
| 1 | Android project setup + static UI | built, awaiting user approval | Navigation Compose added, WorkConfig, Routes, Home per LAYOUT.md (6 cards in order; 2,4,5,6 clickable), placeholder screens with back button, SampleData, Material 3 theme, MainActivity replaced with navigation host. Builds successfully. |
| 2 | Room + daily strokes (Sections 1, 2) | done | Room entities/DAOs/repo/validators/unit tests all present and passing. |
| 3 | Earnings engine (Sections 3, 4) | not started | |
| 4 | Payments (Sections 5, 6) | not started | |
| 5A | Supabase schema + RLS | not started | |
| 5B | Invitations + 120-user limit | not started | |
| 5C | Backend verification (user runs SQL) | not started | |
| 6A | Google OAuth config + Auth client | not started | |
| 6B | Session, access denied, account isolation | not started | |
| 7A | Sync foundation + push | not started | |
| 7B | Pull, restore, conflicts, status UI | not started | |
| 8 | Backup and recovery | not started | |
| 9 | Final validation + release APK | not started | |

## Log
- Task 1 (corrected): Updated MainActivity to use TheCountApp (replaced Greeting). Navigation host and all screens already created; confirmed Home matches layout (6 numbered cards in order, cards 2,4,5,6 clickable, back buttons on placeholders, Settings links to login). Kept Task 2 Room code intact.
- Build: assembleDebug - BUILD SUCCESSFUL. Unit tests: testDebugUnitTest - BUILD SUCCESSFUL (ValidatorsTest 9/9 passed).

## Manual setup done by the user
- [ ] Android Studio + base project created
- [ ] Supabase project created, URL + anon key in local.properties
- [ ] SQL migrations run in Supabase
- [ ] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

