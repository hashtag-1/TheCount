Task 2 UI wiring complete

Changes
- HomeViewModel: added saveForDate/delete/error state
- ViewModelFactory: new factory for injecting Room
- EntryScreen: wired to save/update with validation; shows saved count for date
- CalendarScreen: lists from 2026-09-02 to today, "No entry" styling, tap to edit, delete with confirmation
- HomeScreen: shows real today count, selected date count, cumulative with grouping (NumberFormat); Sections 3-6 still samples
- TheCountApp: provides ViewModel, wires navigation

Build/tests (real)
- assembleDebug: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL; ValidatorsTest 9/9 passed (2026-10-11T17:26:34)

Task 2 marked done in STATE.md. Sections 1-2 now use Room data.

STOP - awaiting approval.
