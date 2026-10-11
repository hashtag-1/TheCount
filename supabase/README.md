# Supabase: Migrations (Task 5B additions)

DO NOT edit 001_schema.sql, 002_triggers.sql or 003_rls.sql. Add 004 and 005 only.

## Files
- 004_invitations.sql: app_config max_users seed, active user count helper, ensure active-email index.
- 005_registration_guard.sql: pending_registrations table (coordination), AFTER INSERT trigger on auth.users to create profile only when authorized; designed to work with Before User Created Auth Hook.

## How to enable Before User Created Auth Hook (recommended)
This is the critical part: Before User Created runs before auth.users is created, so reject uninvited/over-limit signups before account exists.

1. In Supabase Dashboard > Authentication > Hooks > New Hook
2. Type: Before User Created
3. Edge Function (recommended): create a function that:
   - Receives event with email (normalize to lowercase)
   - Runs in a transaction: acquire advisory lock (e.g. pg_advisory_xact_lock(hashtext(lower(email)))) and also lock app_config/max_users
   - Check for active invitation: exists (select 1 from invitations where email=lower and status='active' and (expires_at is null or expires_at>now))
   - Check user count: (select count(*) from profiles where deleted_at is null) < (select value::int from app_config where key='max_users')
   - If valid: mark invitation as used? Or create pending reservation (use pending_registrations) so it can't be reused; atomically reserve slot
   - On success: allow signup (don't create profile here). Profile created by AFTER INSERT on auth.users (trigger above) if reservation matches.
   - On failure: raise error with clear message (e.g. "Not invited", "Account limit reached", "Invitation expired/revoked")
4. The BEFORE hook must enforce in one transaction with lock: count profiles, check active invitation, consume/reserve invitation, ensure < max_users. If two concurrent signups, only one gets lock.
5. Also ensure Google sign-in path goes through same hook (Supabase Auth triggers hooks for email/password and OAuth signups).

## Exact SQL commands (run in SQL Editor as needed)

- Invite an email: 
  `insert into invitations (email) values (lower('user@example.com'));`
- List invitations:
  `select id, email, status, created_at, expires_at, used_at, used_by from invitations order by created_at desc;`
- Revoke one (by id or email):
  `update invitations set status='revoked', expires_at=now() where id='<id>' and status='active';` or by email
- Change max_users:
  `insert into app_config (key,value) values ('max_users','<n>') on conflict (key) do update set value=excluded.value;`
- Set max_users to 120:
  `insert into app_config (key,value) values ('max_users','120') on conflict (key) do update set value='120';`
- Count users (active profiles):
  `select thecount_active_user_count() as active_users;` or `select count(*) from profiles where deleted_at is null;`

## Important
- Invitations matched in lowercase. Active invitation per email enforced by partial unique index.
- Used invitations cannot create second account (consumed/reserved). 
- Profiles created ONLY by server (trigger on auth.users after authorized signup). No app-side profile creation.
- Enforcement must happen in one transaction with advisory lock to prevent last-slot race.
- Cannot run SQL here; do not claim it works. RLS unchanged for existing tables.

See supabase/README.md for run order (004 after 003, 005 after 004).
