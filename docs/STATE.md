# Project State

The agent updates this after every task. Mark a task done only after the user approves it.

| Task | What | Status | Notes |
|---|---|---|---|
| 1 | Android project setup + static UI | done | Navigation Compose added, WorkConfig, Routes, Home per LAYOUT.md (6 cards in order; 2,4,5,6 clickable), placeholder screens with back button, SampleData, Material 3 theme, MainActivity replaced with navigation host. Builds successfully. User verified on device. |
| 2 | Room + daily strokes (Sections 1, 2) | done | Room wired to UI: Home shows real counts (today, selected date, cumulative). EntryScreen saves/updates via validators. Calendar shows history, no-entry dates look different, tapping opens for editing, delete with confirmation. Shared selectedDate in ViewModel. upsertOrRevive revives soft-deleted rows (D6). Sections 3-6 still show sample values. All validator tests pass. |
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
- Task 1: corrected - MainActivity replaced with navigation host. Verified build OK.
- Task 2: Room + wiring complete. 9 validator tests pass. UI shows real data, validators enforce rules, soft delete + revive working.
- Housekeeping: Task 1 marked done (user verified), Task 2 audited against checklist - all PASS. Git commit: "Tasks 1 and 2 done" (b157e63).

## Manual setup done by the user
- [ ] Android Studio + base project created
- [ ] Supabase project created, URL + anon key in local.properties
- [ ] SQL migrations run in Supabase
- [ ] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

