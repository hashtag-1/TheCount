# TheCount: Project Rules

Native Android personal stroke-earning tracker. Kotlin, Jetpack Compose, Material 3.
Offline-first (Room), cloud sync + Google login via Supabase, invitation-only, max 120 users.

## Every session
Read docs/STATE.md, then the current task in docs/prompts.md.
Reference: docs/SPEC.md (full requirements), docs/ARCHITECTURE.md (how parts connect),
docs/DECISIONS.md (agreed choices), docs/ui/LAYOUT.md (screens).

## Fixed constants (one place: domain/WorkConfig.kt)
Start date 2026-09-02 | rate 0.04 रु per stroke | tax 1% of cumulative gross | symbol "रु" | 2 decimals | max users 120.

## Hard rules
1. Money: BigDecimal only, never Double/Float. HALF_UP, scale 2. ONE EarningsCalculator does all money math; UI never computes money.
2. Cumulative strokes, gross, tax, net, outstanding are DERIVED. Never stored, never editable.
3. Work dates: java.time.LocalDate, stored as ISO text "yyyy-MM-dd". No epoch/UTC conversion for work dates. "Today" = LocalDate.now() on the device (Nepal, UTC+5:45).
4. Layers: Compose UI -> ViewModel -> Repository -> Room (later + Remote). UI never touches a DAO or the network.
5. Local-first: write to Room first, sync later. Never show "backed up" until the server confirms.
6. Records use UUID ids, soft delete (deletedAt) and sync columns from the start.
7. Secrets: never commit keys. Supabase URL, anon key and Google web client id come from local.properties via BuildConfig. Service-role key and OAuth client secret never go in the app or repo.
8. Never invent credentials, URLs or test results. Never call an untested feature complete.

## Process
- One task at a time, in order. Never start the next without my approval.
- Inspect before changing. Build incrementally. Do not regenerate existing files.
- Windows/PowerShell: use .\gradlew.bat. Build: assembleDebug. Unit tests: testDebugUnitTest.
- Write each file in its own write call with filePath and full content. If a write fails, retry that one file once, then STOP and tell me.
- No throwaway scripts outside the project. No web research unless the task says so.
- If a step takes about 10 minutes or fails 3 times, STOP and ask me.
- Keep reasoning short. Pick the simplest option and record it in docs/DECISIONS.md.
- After each task: list files changed, what was really tested and the results, and any manual steps for me. Update docs/STATE.md. STOP.
