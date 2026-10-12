# Backend verification (Task 5C)

Two SQL files you run by hand in the Supabase SQL Editor. Each prints one result grid:
every check is PASS or FAIL, and the last row is SUMMARY.

## Safety (read once)
- Everything the tests do (fake users, invitations, a temporary change to max_users) happens in
  a block that is ALWAYS rolled back, even if a check crashes. Real users and data are not touched.
  Test emails all end in @thecount-test.invalid.
- Never paste your database password or connection string anywhere. These tests need neither.
- After each run, confirm nothing changed: run `select * from public.app_config;`
  and check max_users is still 120.

## Run order
1. `registration_guard.sql`  (invitations, Google-only, the user limit)
2. `rls_isolation.sql`       (users cannot see or change each other's data)

## Click by click, for each file
1. In Android Studio (Project view) open the file under `supabase/tests/`.
2. Click inside, Ctrl+A, Ctrl+C. The first line must start with `-- registration_guard.sql`
   or `-- rls_isolation.sql` (do NOT copy just the file name).
3. Supabase dashboard -> project `thecount` -> SQL Editor -> New query -> click in the editor -> Ctrl+V.
4. Click Run. If Supabase warns about "destructive operations", click Run this query
   (the script contains delete/update statements that only touch the rolled-back test data).
5. Look at the result grid at the bottom (scroll down in it). Screenshot the whole grid, or copy it.

## What you should see
- registration_guard.sql: 24 checks, SUMMARY row = PASS, `passed 24, failed 0`
- rls_isolation.sql:      52 checks, SUMMARY row = PASS, `passed 52, failed 0`

A FAIL row shows what happened. Do not "fix" the test. Send me the whole grid.
A row named `SETUP step N` or `TEST CRASHED` means the script itself hit an error on your
project (for example a column that differs in the real auth schema). Send me the detail text.

## What these tests do NOT prove
- Many sign-ups at the same instant. The guard uses a database lock for this (tested on a
  local Postgres: 10 simultaneous sign-ups with a limit of 3 let in exactly 3), but it is not
  re-tested on your project. A Node test that needs your database password was removed on purpose.
- Real Google sign-in. That is proven in Task 6B on your phone.
- That the "Before User Created" hook is enabled in the dashboard. Only the hook function is tested.
