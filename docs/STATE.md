# Project State

The agent updates this after every task. Mark a task done only after the user approves it.

| Task | What | Status | Notes |
|---|---|---|---|
| 1 | Android project setup + static UI | done | Navigation Compose added, WorkConfig, Routes, Home per LAYOUT.md, placeholders, SampleData, Material 3 theme, MainActivity replaced with navigation host. Built successfully. User verified on device. |
| 2 | Room + daily strokes (Sections 1, 2) | done | Room wired to UI; validators all pass (9 tests). Soft delete/revive, no-dup on edit, persistent DB. Sections 3-6 still samples. |
| 3 | Earnings engine (Sections 3, 4) | done | EarningsCalculator (pure) with BigDecimal HALF_UP, scale 2. HomeViewModel computes earnings for selected date; UI shows real values. All tests pass (19 total). User approved. |
| 4 | Payments (Sections 5, 6) | done | Payments end-to-end local: entity/dao/repo, validators, DB v2 no destructive migration, earnings include real payments, UI shows totals/outstanding with status, add/list/delete with confirmation. User approved. |
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
- Task 4: completed, tested, approved.

## Manual setup done by the user
- [ ] Android Studio + base project created
- [ ] Supabase project created, URL + anon key in local.properties
- [ ] SQL migrations run in Supabase
- [ ] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

