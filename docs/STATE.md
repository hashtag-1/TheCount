# Project State

The agent updates this after every task. Mark a task done only after the user approves it.

| Task | What | Status | Notes |
|---|---|---|---|
| 1 | Android project setup + static UI | built, awaiting user approval | Navigation Compose added, WorkConfig, Routes, Home per LAYOUT.md (6 cards in order; 2,4,5,6 clickable), placeholder screens with back button, SampleData, Material 3 theme, MainActivity replaced with navigation host. Builds successfully. |
| 2 | Room + daily strokes (Sections 1, 2) | done | Room wired to UI: Home shows real counts (today, selected date, cumulative). EntryScreen saves/updates via validators. Calendar shows history, no-entry dates look different, tapping opens for editing, delete with confirmation. Shared selectedDate in ViewModel. Sections 3-6 still show sample values. All 9 validator tests pass. |
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
- Task 1 (corrected): MainActivity replaced with navigation host. All files present.
- Task 2 (wiring): Extended HomeViewModel with saveForDate, delete, error state; created ViewModelFactory; rewired HomeScreen to show real data (today count, selected date count, cumulative with grouping). EntryScreen wired to ViewModel with validation and Save/Update. CalendarScreen lists dates from 2026-09-02 to today, shows "No entry" for dates without data, allows edit on tap, delete with confirmation. AppNavHost passes ViewModel and handles navigation. Kept Sections 3-6 using SampleData.

## Manual setup done by the user
- [ ] Android Studio + base project created
- [ ] Supabase project created, URL + anon key in local.properties
- [ ] SQL migrations run in Supabase
- [ ] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

