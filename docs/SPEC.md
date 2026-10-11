<!-- Original requirements, kept verbatim. Do not edit. Changes go in docs/DECISIONS.md -->

# PROJECT: THECOUNT — PERSONAL STROKE EARNING TRACKER

You are my senior Android developer. Build a minimal, reliable, secure Android application named **TheCount** for tracking data-entry strokes, calculating earnings, recording payments, and monitoring outstanding income.

I will develop this application using AI-assisted vibe coding in Google Antigravity. You must implement it incrementally, following the numbered tasks in this specification.

**CRITICAL EXECUTION RULE: Start with Task 1 only. Do not implement Task 2 or any subsequent task until I explicitly approve the completed current task.**

Do not attempt to generate the entire application in one response.

---

# PART A — FINAL REQUIREMENTS

## A1. Application details

- Application name: TheCount
- Platform: Native Android APK
- Language: Kotlin
- UI framework: Jetpack Compose
- UI design: Material 3
- Local database: Room
- Cloud database: Supabase PostgreSQL
- Authentication: Supabase Auth with Google OAuth
- Security: PostgreSQL Row Level Security (RLS)
- Offline support: Yes
- Cloud synchronization: Yes
- Maximum registered users: 120
- Registration policy: Invitation-only
- Work start date: September 2, 2026
- Currency: Nepalese rupees, displayed as `रु`
- Stroke rate: रु 0.04 per stroke
- Tax rate: 1% of cumulative gross earnings
- Monetary display: Two decimal places

This is a personal earning tracker, not a banking application. Do not add unnecessary features, advertising, subscriptions, social features, or complicated dashboards.

## A2. Core application behavior

The home screen must contain six numbered sections based on my UI reference sketches.

### Section 1 — Enter Today's Stroke Count

This section must allow the user to enter the stroke count for the current date.

Requirements:
- Display today's date.
- Provide a numeric input field for a non-negative integer.
- Include a Save or Update button.
- Display the currently saved count for today.
- If a count already exists, allow the user to edit it.
- Save the entry locally immediately.
- Synchronize it with Supabase when online.
- Prevent duplicate daily entries for the same user and date.
- Reject invalid negative or fractional stroke counts.

The work start date is September 2, 2026. Do not permit entries dated before this date.

### Section 2 — Daily Stroke Count

Display a calendar or date-based history of saved daily stroke counts.

Requirements:
- Allow navigation between dates.
- Start the available work history on September 2, 2026.
- Show the saved count for each date.
- Clearly distinguish dates with entries from dates without entries.
- Allow the user to open a previous date and correct its stroke count.
- Recalculate all dependent earnings after any edit.
- Do not silently interpret a missing entry as a confirmed zero-stroke workday.

### Section 3 — Cumulative Total Stroke Count

Display cumulative strokes from September 2, 2026, through the selected date.

Formula:

Cumulative strokes = sum of all saved daily stroke counts on or before the selected date.

Requirements:
- Update immediately when a daily entry is saved, edited, or deleted.
- Include only the authenticated user's records.
- Do not maintain a separately editable cumulative total.
- Display the total as an integer with readable digit grouping.

### Section 4 — Total Earning

Make this section clickable.

Opening it must display a dedicated earnings breakdown with exactly these three primary values.

**4.1 Total Earning**

Cumulative strokes multiplied by रु 0.04.

**4.2 Tax = 1%**

Calculate 1% of the cumulative gross earning.

**4.3 Receivable Income**

Gross earning minus tax.

Show the selected date or calculation period so the user understands which records are included.

All amounts must be calculated automatically. Do not allow users to manually overwrite these calculated values.

### Section 5 — Payment Received

The user receives payment after tax deduction. Record the actual net amount received from the employer.

Provide an Add Payment screen or dialog with:
- Payment amount.
- Date received.
- Optional note or reference.
- Save and Cancel buttons.

Requirements:
- Allow multiple payment records, including multiple payments on the same date.
- Display payment history sorted by date, newest first.
- Allow the user to edit or delete an existing payment with confirmation.
- Recalculate the outstanding balance after payment changes.
- Store every payment separately.
- Save changes locally immediately and synchronize them with the cloud when possible.

Do not automatically deduct the tax a second time from recorded payments.

### Section 6 — Payment Yet to Be Received

Display the remaining amount owed to the user.

Formula:

Outstanding balance = cumulative net receivable income − cumulative payments received.

Use payments dated on or before the selected calculation date.

Requirements:
- A positive amount means money is still owed.
- Zero means fully settled.
- A negative amount means payments exceed the calculated receivable; display it clearly as an overpayment or credit.
- Never silently clamp a negative balance to zero.
- Update automatically when strokes or payments change.

---

# PART B — FINANCIAL CALCULATION RULES

Use a single calculation service for all earnings calculations. Do not duplicate formulas across UI components.

For a selected calculation date D:

S = sum of saved strokes dated from September 2, 2026 through D.

Gross = S × 0.04

Tax = round(Gross × 0.01, 2 decimal places)

Net receivable = Gross − Tax

Payments = sum of recorded payment amounts whose payment dates are on or before D.

Outstanding = Net receivable − Payments

## Monetary precision

- Use Kotlin BigDecimal for financial calculations.
- Use PostgreSQL NUMERIC for monetary values.
- Never use floating-point Double or Float for financial calculations.
- Use decimal-safe parsing and arithmetic.
- Apply HALF_UP rounding to two decimal places for displayed gross, tax, net receivable, and outstanding amounts.
- Calculate tax on cumulative gross earnings, not on individually rounded daily taxes.
- Ensure that the same inputs produce identical results on Android and PostgreSQL.
- Store raw stroke counts as integers.

Use this test case:

Cumulative strokes: 100,000

Expected values:
- Gross earning: रु 4,000.00
- Tax: रु 40.00
- Net receivable: रु 3,960.00
- Payments received: रु 2,000.00
- Outstanding: रु 1,960.00

Add unit tests for zero strokes, one stroke, large counts, tax rounding boundaries, edited historical entries, multiple payments, and negative outstanding balances.

---

# PART C — DATABASE DESIGN

Use Supabase PostgreSQL as the authoritative cloud database and Room as the local database.

Design a normalized schema containing at least the following logical tables.

## C1. Profiles

Fields:
- user_id: UUID, matching the authenticated Supabase user.
- display_name.
- email.
- created_at.
- updated_at.

Use the authenticated user's ID as the profile's unique identifier.

## C2. Daily strokes

Fields:
- id: UUID.
- user_id: UUID.
- work_date: DATE.
- stroke_count: BIGINT or another appropriately constrained integer type.
- created_at.
- updated_at.
- deleted_at, if using soft deletion for synchronization.

Add a unique constraint on `(user_id, work_date)`.

A user can have at most one daily stroke record per date.

## C3. Payments

Fields:
- id: UUID.
- user_id: UUID.
- payment_date: DATE.
- amount: NUMERIC with appropriate precision.
- note: nullable text.
- created_at.
- updated_at.
- deleted_at, if using soft deletion for synchronization.

Multiple payment records may exist for the same user and date.

## C4. Invitations

Create a secure invitation mechanism for authorized registration.

Invitation records should include:
- Invited email address.
- Invitation status.
- Creation timestamp.
- Optional expiration timestamp.
- Optional invitation identifier.

Only authorized administrators may create or revoke invitations.

An invitation must not be reusable by arbitrary users or allow duplicate account creation.

## C5. Synchronization metadata

Track the information needed to synchronize local changes safely, such as:
- Record identifier.
- User identifier.
- Last local modification.
- Last successful synchronization.
- Pending operation status.
- Server version or conflict metadata, where appropriate.

Keep sensitive authentication tokens under the supported secure authentication storage mechanism. Never store them as ordinary application preferences.

Use UUIDs for records that may be created offline.

Do not store cumulative strokes, gross earnings, tax, net receivable, or outstanding balance as independently editable database values. Derive them from daily strokes and payments.

---

# PART D — GOOGLE AUTHENTICATION AND INVITATION-ONLY ACCESS

Implement Google sign-in using Supabase Auth and the currently supported native Android OAuth flow.

Requirements:
- Sign in with Google.
- Restore the existing session securely.
- Sign out.
- Handle cancelled sign-in and network errors.
- Display a useful error for accounts without permission.
- Restore records belonging to the authenticated account.
- Never expose another user's records.

## Maximum 120 registered accounts

The application must enforce invitation-only registration and a hard limit of 120 registered accounts.

IMPORTANT: Hiding a sign-up button or checking the account count only inside Android is not sufficient.

Implement the most secure supported server-side registration mechanism. Evaluate Supabase's supported authentication hooks, database triggers, or an Edge Function-based registration flow.

The implementation must:
1. Verify that the Google-authenticated email is invited.
2. Validate that the invitation is active and eligible for use.
3. Enforce the 120-account limit atomically on the server.
4. Prevent concurrent registrations from exceeding the limit.
5. Prevent unauthorized users from creating usable profiles or accessing application data.
6. Reject uninvited accounts and explain how access can be requested.
7. Document how an administrator invites users and revokes invitations.

If an authentication provider creates a provisional account before the invitation check completes, prevent that account from gaining application access and document a safe cleanup strategy.

Do not claim the 120-user limit is enforced until the backend implementation and concurrent-registration behavior have been tested.

## RLS security

Enable Row Level Security on every user-owned table.

Policies must enforce that:
- Users can read only their own profile.
- Users can read and modify only their own daily stroke records.
- Users can read and modify only their own payments.
- Users cannot create or approve their own invitations.
- Ordinary users cannot access another user's records by changing a UUID.
- Administrative privileges cannot be obtained by changing client-side fields.

Never place a Supabase service-role key, OAuth client secret, or other privileged secret in the APK.

The Supabase URL and publishable/anon key may be included in the Android configuration only when protected by correct RLS and server-side authorization.

---

# PART E — LOCAL STORAGE AND CLOUD SYNCHRONIZATION

The app must remain useful when offline.

## Local-first behavior

- Save every stroke and payment change to Room first.
- Update the interface immediately after a successful local save.
- Track pending cloud changes.
- Retry synchronization when connectivity returns.
- Show whether data is synchronized, pending, or affected by an error.
- Never report a successful cloud backup before the server confirms the change.

## Safe synchronization

Implement synchronization for:
- New stroke records.
- Edited stroke records.
- Deleted stroke records.
- New payments.
- Edited payments.
- Deleted payments.
- Initial account data restoration.

Use stable UUIDs, server-side ownership checks, idempotent writes, and version or conflict handling.

Do not rely exclusively on device timestamps to determine which record wins.

If the same record changes on two devices, apply a documented conflict policy. Avoid silently discarding financial records.

Use soft deletion or an equivalent synchronization-safe deletion mechanism so deleted records do not reappear when another device syncs.

## New phone recovery

When the user signs into a new phone:
- Authenticate the Google account.
- Download only the data belonging to that account.
- Restore stroke records and payment history.
- Recalculate all earnings from restored data.
- Do not merge unrelated local data automatically.
- A new Google account must start with empty application records.

If the new phone has no local records, do not mistake that empty local database for an instruction to delete the cloud records.

## Backup and recovery

Cloud synchronization is not the same as an independent backup.

Provide a simple export/import backup feature if practical, and document any limitations of the selected Supabase plan.

A restore operation must validate the file, enforce user ownership, prevent accidental duplication, and clearly explain whether existing records will be replaced or merged.

---

# PART F — USER INTERFACE

Use the two supplied reference sketches as the functional layout guide.

The design should be minimal, readable, and optimized for a mobile phone.

## Home screen

Display:
1. TheCount title.
2. Current date.
3. Account and synchronization status.
4. Section 1: Enter Today's Stroke Count.
5. Section 2: Daily Stroke Count.
6. Section 3: Cumulative Total Stroke Count.
7. Section 4: Total Earning.
8. Section 5: Payment Received.
9. Section 6: Payment Yet to Be Received.

Each section should be a compact clickable card.

Sections 1–6 must remain easy to identify, following the numbering in the reference sketch.

## Detailed screens

Create separate screens for:
- Login.
- Home.
- Daily stroke entry.
- Calendar and historical stroke records.
- Earnings breakdown.
- Payment entry and payment history.
- Settings and account information.
- Invitation/access-denied messages, where needed.

Use sensible Android navigation and back-button behavior.

## UI principles

- Use Material 3.
- Use a restrained neutral color palette.
- Use consistent spacing and typography.
- Make monetary values prominent.
- Use `रु` consistently.
- Make forms comfortable to use on small screens.
- Add loading, empty, error, and offline states.
- Ask for confirmation before destructive actions.
- Avoid unnecessary charts, animations, or decorative widgets.

---

# PART G — SEQUENTIAL ANTIGRAVITY TASKS

Complete these tasks in order. Do not skip ahead.

## TASK 1 — Native Android project setup and static UI

This is the ONLY task to execute initially.

Actions:
1. Inspect the workspace and existing files.
2. Preserve any existing project work.
3. Create or configure the Kotlin Android project.
4. Configure Jetpack Compose and Material 3.
5. Set the application name to TheCount.
6. Build the basic navigation structure.
7. Create the home screen with the six numbered sections.
8. Add simple placeholder screens for the major destinations.
9. Use sample data only where necessary.
10. Ensure the interface follows the supplied sketches.
11. Build and run the app if the Android SDK and emulator or device are available.
12. Fix all build errors before reporting completion.

Do not implement Room, Supabase, Google login, or cloud synchronization during Task 1.

Deliverables:
- Working Android project.
- Six-section home screen.
- Basic navigation.
- Responsive layouts.
- Successful build or a precise explanation of any missing environment dependency.

Stop after Task 1 and ask for my approval.

## TASK 2 — Room database and daily stroke history

After approval:
- Add Room and its required dependencies.
- Create entities, DAOs, database configuration, and repositories.
- Implement today's stroke entry.
- Implement date selection and historical editing.
- Enforce the work start date.
- Test local persistence after closing and reopening the app.

## TASK 3 — Earnings engine

After approval:
- Implement the central calculation service.
- Use BigDecimal.
- Implement cumulative stroke totals.
- Calculate gross earning, tax, net receivable, and outstanding balance.
- Add automated unit tests.
- Verify that historical corrections update the results correctly.

## TASK 4 — Payment tracking

After approval:
- Implement payment entry, history, editing, and deletion.
- Support multiple payments on the same date.
- Implement correct date-filtered totals.
- Recalculate the outstanding balance immediately.
- Test negative outstanding balances and overpayments.

## TASK 5 — Supabase database and security

After approval:
- Create SQL migrations for the schema.
- Configure constraints and indexes.
- Enable RLS on all user-owned tables.
- Implement the invitation mechanism.
- Enforce the 120-user limit on the server.
- Provide instructions for configuring the Supabase project.
- Test unauthorized database access and account isolation.

Do not place privileged keys in client code.

## TASK 6 — Google authentication

After approval:
- Configure Supabase Auth.
- Integrate Google OAuth.
- Implement sign-in, session restoration, sign-out, and access-denied handling.
- Verify invited and uninvited account behavior.
- Test account isolation.

## TASK 7 — Offline synchronization

After approval:
- Implement Room-to-Supabase synchronization.
- Handle pending writes, retries, conflicts, and deletions.
- Prevent duplicate records.
- Show synchronization status.
- Restore data on a new phone.
- Ensure that one account never receives another account's records.

## TASK 8 — Backup and recovery

After approval:
- Implement export/import if feasible.
- Validate backup files.
- Protect existing records from accidental replacement.
- Document cloud backup limitations.
- Test recovery after reinstalling the app and signing in again.

## TASK 9 — Final validation and release APK

After approval:
- Test all calculations.
- Test date boundaries.
- Test historical edits.
- Test payment history.
- Test login and invitation restrictions.
- Test the 120-account limit and concurrent registration.
- Test RLS isolation.
- Test offline use and reconnection.
- Test new-device restoration.
- Test fresh installation.
- Build a release APK.
- Provide installation instructions and a concise README.

Never report an untested feature as complete.

---

# PART H — DEVELOPMENT WORKFLOW

For each task:

1. Briefly explain the task.
2. Inspect relevant files before changing them.
3. Implement only the current task.
4. Run the relevant build and tests.
5. Fix errors before proceeding.
6. Report the files created or modified.
7. Report what was tested and the actual results.
8. Clearly list anything requiring manual configuration.
9. Wait for my explicit approval before continuing.

Do not regenerate the entire project at every stage.

Keep the code modular and understandable. Prefer simple repositories, view models, composable screens, and a dedicated calculation service rather than unnecessary abstraction.

Do not invent credentials, database URLs, OAuth settings, test results, or deployment success.

Where dashboard setup is necessary, give me exact instructions for the required configuration.

If a tool, SDK, emulator, or dependency is unavailable, explain the specific limitation and continue only with work that can be verified.

# FINAL INSTRUCTION

**Begin Task 1 only.**

Inspect the workspace, create or configure the Android project, implement the basic TheCount home screen with its six numbered sections and navigation, and verify the build.

Do not start Task 2 until I approve Task 1.