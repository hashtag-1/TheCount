# Supabase: migrations (Tasks 5A and 5B, reviewed)

You run these by hand in the Supabase dashboard (SQL Editor). Nothing here connects to your
project by itself. Never put the service-role key, database password or connection string
in this folder.

## Run order (each file ONCE, in this order)
1. `migrations/001_schema.sql`
2. `migrations/002_triggers.sql`
3. `migrations/003_rls.sql`  (ends with a table: all 6 tables must show rls_enabled = true)
4. `migrations/004_invitations.sql`
5. `migrations/005_registration_guard.sql`

If a file fails, STOP and copy the exact error. Do not run the next one.
Files 004/005 are safe even if you ran an earlier draft of them (they drop its leftovers).

## One-time dashboard settings (after 005)
- Authentication -> Providers: turn OFF Email (and Phone). Only Google should be enabled.
  (The guard also rejects non-Google sign-ups, this is a second lock.)
- Authentication -> Hooks -> Before User Created (OPTIONAL): choose Postgres function
  `public.hook_before_user_created`. It only gives the app a clearer "not invited" /
  "limit reached" message. The real enforcement is the trigger and works without it.

## BEFORE YOU FIRST SIGN IN: invite yourself
Registration is closed until an invitation exists. Run the "invite" SQL below with YOUR own
Google email, exactly as Google shows it, then sign in from the app (Task 6).

## What the guard does
A trigger on auth.users runs in the same transaction that creates the account. Under one
global lock it checks: Google sign-in, an active unexpired invitation for that email
(lowercase), and accounts < max_users. If anything fails it raises an error, the transaction
rolls back and NO account exists. If all pass it marks the invitation used and creates the
profile (status active) in the same transaction. Two simultaneous sign-ups cannot both take
the last slot. The app can never create a profile (no insert grant or policy), so without a
profile every data table is empty and read-only-nothing for that user.

Errors start with: THECOUNT_NOT_INVITED, THECOUNT_LIMIT_REACHED or THECOUNT_DENIED.
Without the hook, the app may only see a generic "Database error saving new user"; either way
sign-up failed and the app should show Access Denied (Task 6B).

### What if something fails halfway?
Nothing is left half-done: the check, the invitation update and the profile insert all
commit or all roll back together. As a safety check, this lists accounts that have no profile
(for example accounts created before 005 was installed). They have no data access:
```sql
select u.id, u.email, u.created_at
from auth.users u left join public.profiles p on p.id = u.id
where p.id is null;
```
Cleanup of such an account (only if you are sure): `delete from auth.users where id = '...';`

## Admin guide (run in SQL Editor; no admin screen exists, D7)
Invite an email (one active invitation per email; a duplicate gives an error):
```sql
insert into public.invitations (email) values (lower('person@gmail.com'));
-- optional expiry: insert into public.invitations (email, expires_at)
--   values (lower('person@gmail.com'), now() + interval '14 days');
```
List invitations:
```sql
select email, status, created_at, expires_at, used_at from public.invitations order by created_at desc;
```
Revoke an unused invitation:
```sql
update public.invitations set status = 'revoked'
where email = lower('person@gmail.com') and status = 'active';
```
Block someone who already registered (they keep their data, but see nothing and cannot sync;
they still occupy a slot):
```sql
update public.profiles set status = 'suspended' where email = lower('person@gmail.com');
-- undo: set status = 'active'
```
Remove a user completely and free the slot (DELETES THEIR CLOUD DATA, cannot be undone):
```sql
delete from auth.users where email = lower('person@gmail.com');
```
That email needs a new invitation to join again (an invitation is single use).

Count accounts / see the limit:
```sql
select public.thecount_user_count() as accounts,
       (select value from public.app_config where key = 'max_users') as max_users;
```
Change the limit (also use 120 to restore it):
```sql
update public.app_config set value = '120' where key = 'max_users';
```
Gmail note: invite the address exactly as it appears in the person's Google account.

## Not proven yet
These were tested on a local Postgres with a mock of Supabase's auth schema (uninvited,
expired, revoked, reused, over-limit, wrong provider, and 10 simultaneous sign-ups with a
limit of 3: exactly 3 got in). They have NOT been run on your real project. Task 5C writes
the tests you run there.
