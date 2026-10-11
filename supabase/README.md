# Supabase: migrations (Task 5A, reviewed)

You run these by hand in the Supabase dashboard. Nothing here connects to your project by itself.
Never put the service-role key, database password or connection string in this folder.

## Run order (each file ONCE)
1. Dashboard -> SQL Editor -> New query.
2. Paste `migrations/001_schema.sql` -> Run. Expect "Success. No rows returned".
3. Paste `migrations/002_triggers.sql` -> Run. Expect success.
4. Paste `migrations/003_rls.sql` -> Run. It ends with a table of the 6 tables;
   every row must show `rls_enabled = true`.
5. If a file fails, STOP and copy the exact error. Do not re-run the other files.

## Confirm in the dashboard
Table Editor: profiles, daily_strokes, payments, invitations, admins, app_config all exist and
none shows an "RLS disabled" badge. Authentication -> Policies lists policies only on
profiles (1), daily_strokes (3) and payments (3).

## What these files guarantee
- A user reads/writes only rows where user_id is their own id, and only with an active profile.
- Clients cannot create or edit profiles. The server creates them in Task 5B, so an
  uninvited Google account never gets an active profile and sees nothing.
- Clients cannot forge `version`, `created_at`, `updated_at`, or change `user_id`.
  `version` comes from one global server counter (used as the sync cursor in Task 7).
- No client DELETE. Soft delete = UPDATE deleted_at.
- invitations, admins, app_config: RLS on, no policy, no grant = invisible to the app.
- Server rejects: strokes < 0, dates before 2026-09-02, payment amount <= 0 or > 2 decimals.

## Not proven yet
These were tested on a local Postgres with a mock of Supabase's auth schema, not on your
project. Task 5C writes the tests you run on the real project.
