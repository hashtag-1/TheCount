# Architecture: how the parts connect

Single Gradle module `app`. Keep it simple: ViewModels, repositories, one calculator. No DI framework unless a task asks (a hand-written `AppContainer` in the Application class is enough).

## 1. The one idea that ties everything together

Only TWO kinds of facts are stored: **daily strokes** and **payments**. Everything else (cumulative strokes, gross, tax, net, outstanding) is computed from them by ONE `EarningsCalculator`.

```
 Room: daily_strokes ──┐                          ┌─> Section 3  cumulative strokes
                       ├─> EarningsCalculator ────┼─> Section 4  gross / tax / net (breakdown screen)
 Room: payments ───────┘   (pure Kotlin, BigDecimal)└─> Section 6  outstanding
        │                           ▲
        │ Section 5 lists/edits     │ selected date D (shared state)
        ▼                           │
   PaymentRepository          HomeViewModel
```

Edit a stroke entry or a payment and every dependent section updates by itself, because the screens observe Room Flows and the calculator re-runs. Nothing derived is ever saved.

## 2. Package layout (`com.sandesh.thecount`)

```
MainActivity.kt, TheCountApp.kt (Application, builds AppContainer)
navigation/   Routes.kt, NavGraph.kt
ui/theme/     Color.kt, Type.kt, Theme.kt
ui/components/ SectionCard.kt, MoneyText.kt, SyncChip.kt, ConfirmDialog.kt
ui/home/      HomeScreen.kt, HomeViewModel.kt
ui/entry/     DailyEntryScreen.kt (+ ViewModel)
ui/calendar/  CalendarScreen.kt (+ ViewModel)
ui/earnings/  EarningsScreen.kt (+ ViewModel)
ui/payments/  PaymentsScreen.kt, AddPaymentScreen.kt (+ ViewModels)
ui/settings/  SettingsScreen.kt (+ ViewModel)
ui/login/     LoginScreen.kt, AccessDeniedScreen.kt (+ ViewModel)
domain/       WorkConfig.kt, EarningsCalculator.kt, EarningsResult.kt, Validators.kt
data/local/   Entities.kt, Daos.kt, TheCountDatabase.kt, Converters.kt
data/repository/ StrokeRepository.kt, PaymentRepository.kt, AuthRepository.kt
data/remote/  SupabaseProvider.kt, remote DTOs            (Task 6A/7A)
data/sync/    SyncManager.kt, SyncWorker.kt, ConflictResolver.kt   (Task 7)
data/backup/  BackupExporter.kt, BackupImporter.kt         (Task 8)
util/         DateFormats.kt, MoneyFormat.kt
```
A task creates only the files it needs. Do not create empty future files.

## 3. Layer rules
UI (Compose) -> ViewModel (StateFlow) -> Repository -> Room DAO. Later Repository also talks to Remote through SyncManager. The UI never calls a DAO, the network or the calculator directly with raw formulas; ViewModels call the calculator and expose results.

## 4. Shared selected date
`HomeViewModel` holds `selectedDate` (default today). Sections 2, 3, 4 and 6 all use it. Section 2's arrows and the calendar change it. The earnings breakdown screen receives the same date.
Rules: selectedDate is never before 2026-09-02 or after today.

## 5. Data model

### Room (local, per account via userId)
```
daily_strokes
  id            TEXT PK (UUID)
  userId        TEXT          ("local" until login, see D4)
  workDate      TEXT          ISO yyyy-MM-dd
  strokeCount   INTEGER       >= 0
  createdAt, updatedAt   TEXT ISO instant
  deletedAt     TEXT NULL     soft delete
  syncState     TEXT          SYNCED | PENDING_UPSERT | PENDING_DELETE | CONFLICT | ERROR
  serverVersion INTEGER NULL
  UNIQUE (userId, workDate)   (soft-deleted rows are revived, never duplicated: D6)

payments
  id TEXT PK (UUID), userId, paymentDate TEXT ISO,
  amount TEXT (BigDecimal plain string, 2 decimals), note TEXT NULL,
  createdAt, updatedAt, deletedAt NULL, syncState, serverVersion NULL
  (many payments per date allowed)

sync_meta   userId TEXT PK, lastPulledVersion INTEGER, lastSuccessfulSyncAt TEXT NULL
```
The sync columns exist from Task 2 even though sync arrives in Task 7. That avoids a risky migration on real data later.

### Postgres (Supabase, authoritative)
```
profiles(user_id PK = auth.users.id, display_name, email, status, created_at, updated_at)
daily_strokes(id uuid PK, user_id, work_date date, stroke_count bigint CHECK >= 0 AND work_date >= '2026-09-02',
              created_at, updated_at, deleted_at, version bigint)   UNIQUE(user_id, work_date)
payments(id uuid PK, user_id, payment_date date, amount numeric(14,2) CHECK > 0, note,
         created_at, updated_at, deleted_at, version bigint)
invitations(id, email (lowercase, unique while active), status, created_at, expires_at, used_at, used_by)
admins(user_id or email)           -- no client policy at all; managed in the dashboard
app_config(key, value)             -- max_users = 120
```
Server-side triggers set `updated_at` and bump `version` on every write, so clients cannot forge them.

## 6. Money calculation (Task 3), for selected date D
```
S        = sum(strokeCount) where workDate in [2026-09-02, D] and deletedAt is null
Gross    = S x 0.04                         (BigDecimal, exact)
Tax      = (Gross x 0.01) HALF_UP, scale 2  (on cumulative gross, never per day)
Net      = Gross - Tax
Payments = sum(amount) where paymentDate <= D and deletedAt is null
Outstanding = Net - Payments                (negative = overpaid, never clamped)
```
Test anchors: 100,000 strokes -> 4,000.00 / 40.00 / 3,960.00; payments 2,000.00 -> outstanding 1,960.00.
Tax rounding boundary: 12 strokes -> gross 0.48, tax 0.00; 13 strokes -> gross 0.52, tax 0.01.

## 7. Auth and access (Tasks 5 and 6)
```
Google account -> Supabase Auth (ID token) -> "before user created" check on the SERVER
     invited? (invitations row active + email match)   AND   user count < max_users (atomic, advisory lock)
        yes -> profile created, status active -> RLS lets the user touch ONLY own rows
        no  -> sign-up rejected -> app shows Access Denied with how to request access
```
RLS on every table: `user_id = auth.uid()` AND the caller has an active profile. Ordinary users have no insert/update/delete policy on invitations, admins or app_config.

## 8. Sync state machine (Task 7)
```
local write -> PENDING_UPSERT / PENDING_DELETE
push (WorkManager, needs network) -> server accepts -> SYNCED (store new serverVersion)
                                  -> server version differs from base -> CONFLICT (user chooses)
                                  -> network/server error -> ERROR (retry with backoff)
pull (cursor = last server version) -> apply SYNCED rows; never overwrite a PENDING row silently
New phone: empty local DB means "download everything", NEVER "delete the cloud".
```

## 9. Task dependency map
```
1 shell ─> 2 Room+strokes ─> 3 calculator ─> 4 payments ─┐
                                                          ├─> 7A/7B sync ─> 8 backup ─> 9 final
5A/5B/5C Supabase backend ─> 6A/6B Google login ──────────┘
```
Tasks 5 can be done in parallel with 2-4 by hand, but keep the agent order. Task 6B needs 5C verified. Task 7 needs 4 and 6B.
