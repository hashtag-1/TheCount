# Decisions

Defaults the agent should follow. Edit a line here BEFORE the task that uses it if you disagree.

| ID | Decision | Used in |
|---|---|---|
| D1 | Package / app id: `com.sandesh.thecount` (change before Task 1; it cannot change after release). minSdk 26. | 1 |
| D2 | Stroke entries cannot be dated in the future. Not before 2026-09-02 (from spec). | 2 |
| D3 | Payments cannot be dated in the future or before 2026-09-02. Amount must be greater than 0 with at most 2 decimals. | 4 |
| D4 | Before login exists (Tasks 2-5), records use `userId = "local"`. On the FIRST sign-in, if "local" records exist, ask once: "Attach N local entries to this account?" Default Yes. This protects the real data you enter before login. A second account never receives them. | 2, 6B |
| D5 | Money is stored in Room as TEXT (BigDecimal plain string, 2 decimals) via a TypeConverter, and as NUMERIC(14,2) in Postgres. | 2, 4, 5A |
| D6 | Soft-deleted rows are revived on re-entry (same date or id), never duplicated, because of the unique (user_id, work_date) rule. | 2, 5A, 7A |
| D7 | Admin tooling = Supabase dashboard (SQL editor). No admin screen in the app. Admin email list lives in an `admins` table that no client can write. | 5B |
| D8 | Max users (120) is a row in an `app_config` table so tests can lower it temporarily. | 5B |
| D9 | Sync conflict policy: server-assigned `version` decides, never device clocks. Same record edited on two devices = CONFLICT, shown to the user to choose. Nothing financial is silently overwritten. | 7B |
| D10 | Sign-out wipes local data ONLY after sync exists (Task 7B) and only when nothing is pending; otherwise it warns and blocks. Before Task 7B, sign-out keeps local data (rows are filtered by userId, nothing is exposed). Everything restores from the cloud on next sign-in. | 6B, 7B |
| D11 | Admin contact shown on the access-denied screen: TO BE SET BY USER (put the email in WorkConfig.ADMIN_CONTACT before Task 6B). | 6B |
| D12 | Release build: minify OFF for the first release APK; turn on only if tested. | 9 |

## Agent-recorded decisions
(Agent appends here when it makes a choice the spec left open.)

| ID | Decision | Used in |
|---|---|---|
| D13 | Auth flow: supabase-kt 3.1.1 with Auth + Postgrest; AndroidX Credential Manager (1.3.0) + Google Identity Services (googleid 1.1.1); Google sign-in uses a nonce - send SHA-256 hex hash of the nonce to Google, pass the raw nonce to Supabase when signing in with ID token. Server rejects handled as AccessDenied (catch messages starting THECOUNT_NOT_INVITED/LIMIT_REACHED/DENIED or generic "Database error saving new user"); network errors -> NetworkError; cancelled Google dialog -> Cancelled. | 6A |