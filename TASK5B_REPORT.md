Task 5B: Invitations and 120-user limit (written only)

Files added
- supabase/migrations/004_invitations.sql: app_config.max_users=120 seed, thecount_active_user_count() helper, ensure active-email partial index on invitations.
- supabase/migrations/005_registration_guard.sql: pending_registrations table for hook coordination, AFTER INSERT trigger on auth.users to create profile only when authorized (server-side profile creation). 

Key points
- Do not edit 001-003 (intact). Added 004, 005 only.
- Profiles created ONLY by server (trigger after authorized signup). No app-side policies to insert/update/delete profiles.
- Before User Created Auth Hook is the recommended enforcement point (runs before auth.users exists). Instructions in README describe: acquire advisory lock in one transaction, check active invitation (lowercased), check count < max_users, reserve slot (via pending_registrations), reject with clear messages on failure. Hook handles OAuth/Google same way.
- Enforcement in one transaction with advisory lock prevents two concurrent signups taking last slot. Invitations matched lowercase, active invitation unique per email, used invitations cannot be reused.
- Exact admin SQL provided: invite, list, revoke, change/set max_users to 120, count active users.

Run status
- Cannot run against Supabase (no credentials). Status: written, NOT yet run or tested.

Android: unchanged, tests still pass. STATE.md updated.
