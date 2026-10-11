Housekeeping audit (Task 2)

a) -5 and 2.5 rejected with plain words
- domain/Validators.kt: validateStrokeCount - rejects non-digit strings (regex `^-?\d+$`), also rejects negative (count < 0). For "-5": regex matches but we check count < 0 -> returns failure "Stroke count cannot be negative" (lines ~10-23). For "2.5": regex fails (contains dot) -> failure "Stroke count must be a whole number" (lines ~15-18). UI shows error via HomeViewModel.error and EntryScreen displays it (EntryScreen.kt lines ~54-58, 60-68; HomeViewModel.kt lines ~54-78). Status: PASS.

b) 2026-09-01 and tomorrow cannot be saved or selected
- Validators.validateWorkDate: rejects if date < START_DATE (2026-09-02) -> "Date cannot be before 2026-09-02" (Validators.kt ~25-37). Rejects if date > today -> "Date cannot be in the future" (line ~33-36). EntryScreen/Calendar/HomeViewModel all call validateWorkDate before save/delete (HomeViewModel.kt ~54-90). Calendar generates dates from START_DATE to today only (CalendarScreen.kt ~55-59), so user cannot navigate to 2026-09-01/tomorrow via UI. Status: PASS.

c) No-entry date shows "No entry" and never treated as 0
- Home shows: todayCount/selectedDateCount shown as "No entry" if null (HomeScreen.kt ~90, ~110). Calendar shows "No entry" with outline color when entry is null (CalendarScreen.kt ~87-91). DAOs return null/Flow values; sum returns COALESCE(SUM(...), 0) only for cumulative (Daos.kt ~49) - cumulative is derived sum; individual dates without entry return no row, so UI treats as null (Daos.kt ~18-20, ~49). No code treats missing date as 0. Status: PASS.

d) Editing older date updates count (no second row; D6)
- upsertOrRevive in Daos.kt: if existing exists (even if soft-deleted) it revives by copying and updating (lines ~33-52) - sets strokeCount, updatedAt, syncState PENDING_UPSERT, deletedAt null; does NOT insert. Unique index (user_id, work_date) prevents duplicates (Entities.kt ~12-27). Status: PASS.

e) Delete asks for confirmation and is soft delete
- CalendarScreen shows AlertDialog with confirm/cancel before deleting (lines ~93-123). HomeViewModel.delete calls strokeRepository.softDelete which calls dao.softDelete setting deleted_at, sync_state='PENDING_DELETE', updated_at (Daos.kt ~55-57). Row kept (soft delete). Status: PASS.

f) Re-entering count for soft-deleted date revives row
- upsertOrRevive fetches by date regardless of deletedAt? getByDate does "SELECT * FROM daily_strokes WHERE user_id=:userId AND work_date=:workDate" with no deletedAt filter (Daos.kt ~18-19), so it finds soft-deleted rows; then copies and sets deletedAt=null and updates (lines ~41-52) - this revives the row. Status: PASS.

g) Sections 1 and 2 read from Room via HomeViewModel; no SampleData for them. 3-6 may use SampleData
- HomeScreen uses uiState.todayCount, uiState.selectedDateCount, uiState.cumulativeStrokes (real data from ViewModel/Room). Sections 3-6 still use SampleData (lines ~120-169). Status: PASS.

h) Data survives closing app (persistent DB)
- TheCountDatabase uses Room.databaseBuilder with name "thecount_db" (not in-memory). Data persisted on disk. Status: PASS.

Unit tests: 9 validator tests all pass (BUILD SUCCESSFUL). No new tests added (existing coverage sufficient per audit).

Build: assembleDebug and testDebugUnitTest both BUILD SUCCESSFUL.

Summary: All items a-h PASS. Task 2 marked done. Git committed.

Done. STOP.
