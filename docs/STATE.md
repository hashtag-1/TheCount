# Project State

The agent updates this after every task. Mark a task done only after the user approves it.

| Task | What | Status | Notes |
|---|---|---|---|
| 1 | Android project setup + static UI | done | Navigation Compose added, WorkConfig, Routes, Home per LAYOUT.md, placeholders, SampleData, Material 3 theme, MainActivity replaced with navigation host. Built successfully. User verified on device. |
| 2 | Room + daily strokes (Sections 1, 2) | done | Room wired to UI; validators all pass (9 tests). Soft delete/revive, no-dup on edit, persistent DB. Sections 3-6 still samples. |
| 3 | Earnings engine (Sections 3, 4) | done | EarningsCalculator (pure) with BigDecimal HALF_UP, scale 2. Formulas: gross=strokes*rate, tax=gross*1% (rounded), net=gross-tax, outstanding=net-payments (never clamped). EarningsResult. Unit tests cover cases including 100000 strokes (4000/40/3960), 12/13 boundary, large count, multiple payments, payments > net. HomeViewModel computes earnings from cumulative strokes up to selected date (payments 0 for now). Section 4 shows real receivable on Home and full breakdown on EarningsScreen. Sections 5-6 still use samples. |
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
- Task 3: Created EarningsResult.kt, EarningsCalculator.kt (BigDecimal only, HALF_UP scale 2). Added MoneyFormat.kt. Created EarningsCalculatorTest with 9 tests (all pass). Updated HomeViewModel to compute earnings from cumulative strokes up to selected date (payments=0). Updated HomeScreen Section 4 to show real receivable income. Updated EarningsScreen to show breakdown with selected date. App compiles; all unit tests pass (ValidatorsTest 9/9, EarningsCalculatorTest 9/9, ExampleUnitTest 1/1).

## Manual setup done by the user
- [ ] Android Studio + base project created
- [ ] Supabase project created, URL + anon key in local.properties
- [ ] SQL migrations run in Supabase
- [ ] Google Cloud OAuth clients created, Google provider enabled in Supabase
- [ ] Release keystore created and backed up

