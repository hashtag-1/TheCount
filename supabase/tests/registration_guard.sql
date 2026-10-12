-- registration_guard.sql  (Task 5C, reviewed rewrite)
-- Proves the invitation guard and the user limit on YOUR Supabase project.
--
-- HOW TO RUN: Supabase -> SQL Editor -> New query -> paste this WHOLE file -> Run.
-- The result grid at the bottom lists every check as PASS or FAIL, and the last row is SUMMARY.
--
-- SAFETY: everything the test does (test users, invitations, a temporary max_users change)
-- happens inside a block that is ALWAYS rolled back, even if a check crashes. Real users and
-- real data are never modified. Test emails end in @thecount-test.invalid.
-- Afterwards run:  select * from public.app_config;   max_users must still be your value (120).

create or replace function pg_temp.thecount_guard_test()
returns table (step int, check_name text, result text, detail text)
language plpgsql
as $fn$
declare
  chk      record;
  n        bigint;
  ok       boolean;
  detail_t text;
  states   text;
  msg      text;
  i        int := 0;
  k        int := 0;
  n_pass   int := 0;
  n_fail   int := 0;
  res      jsonb[] := '{}';
begin
  begin  -- everything in here is rolled back at the end, always
    for chk in
      select * from (values
        -- ===== setup (silent: name is null; must succeed) =====
        (null::text, $q$update public.app_config set value = ((select count(*) from public.profiles) + 50)::text where key = 'max_users'$q$, 'ok'),
        (null, $q$insert into public.invitations (email, status, expires_at) values
            ('ok1@thecount-test.invalid',       'active',  null),
            ('mixedcase@thecount-test.invalid', 'active',  null),
            ('owner@thecount-test.invalid',     'active',  null),
            ('expired@thecount-test.invalid',   'active',  now() - interval '1 day'),
            ('revoked@thecount-test.invalid',   'revoked', null),
            ('nongoogle@thecount-test.invalid', 'active',  null)$q$, 'ok'),

        -- ===== who may register =====
        ('Uninvited email is rejected',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'stranger@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$,
         'err=P0001~THECOUNT_NOT_INVITED'),
        ('An invitation for another email cannot be used by a different email',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'intruder@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$,
         'err=P0001~THECOUNT_NOT_INVITED'),
        ('...and the other email''s invitation is still active',
         $q$select 1 from public.invitations where email = 'owner@thecount-test.invalid' and status = 'active'$q$, 'rows=1'),
        ('Expired invitation is rejected',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'expired@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$,
         'err=P0001~THECOUNT_NOT_INVITED'),
        ('Revoked invitation is rejected',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'revoked@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$,
         'err=P0001~THECOUNT_NOT_INVITED'),
        ('Non-Google sign-in is rejected even with a valid invitation',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'nongoogle@thecount-test.invalid', '{"provider":"email"}'::jsonb)$q$,
         'err=P0001~THECOUNT_DENIED'),
        ('...and that invitation is still active',
         $q$select 1 from public.invitations where email = 'nongoogle@thecount-test.invalid' and status = 'active'$q$, 'rows=1'),

        -- ===== a valid sign-up =====
        ('Valid invitation + Google sign-in is accepted',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'ok1@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$, 'ok'),
        ('Profile was created by the server with status active',
         $q$select 1 from public.profiles where email = 'ok1@thecount-test.invalid' and status = 'active'$q$, 'rows=1'),
        ('Invitation was marked used and linked to the new account',
         $q$select 1 from public.invitations where email = 'ok1@thecount-test.invalid' and status = 'used' and used_by is not null and used_at is not null$q$, 'rows=1'),
        ('The same invitation cannot create a second account',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'ok1@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$,
         'err=P0001~THECOUNT_NOT_INVITED'),
        ('Still exactly one profile for that email',
         $q$select 1 from public.profiles where email = 'ok1@thecount-test.invalid'$q$, 'rows=1'),
        ('Email match ignores upper/lower case (invitation is lowercase)',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'MixedCase@THECOUNT-test.invalid', '{"provider":"google"}'::jsonb)$q$, 'ok'),
        ('...and the profile email was stored in lowercase',
         $q$select 1 from public.profiles where email = 'mixedcase@thecount-test.invalid'$q$, 'rows=1'),

        -- ===== the user limit =====
        (null, $q$update public.app_config set value = ((select count(*) from public.profiles) + 2)::text where key = 'max_users'$q$, 'ok'),
        (null, $q$insert into public.invitations (email) values ('lim1@thecount-test.invalid'), ('lim2@thecount-test.invalid'), ('lim3@thecount-test.invalid')$q$, 'ok'),
        ('Limit: 1st account within the limit is accepted',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'lim1@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$, 'ok'),
        ('Limit: 2nd account within the limit is accepted',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'lim2@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$, 'ok'),
        ('Limit: the next account (over the limit) is rejected',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'lim3@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$,
         'err=P0001~THECOUNT_LIMIT_REACHED'),
        ('...its invitation is still active (slot not consumed)',
         $q$select 1 from public.invitations where email = 'lim3@thecount-test.invalid' and status = 'active'$q$, 'rows=1'),
        ('...and no account or profile was left behind',
         $q$select 1 from auth.users where email = 'lim3@thecount-test.invalid' union all select 1 from public.profiles where email = 'lim3@thecount-test.invalid'$q$, 'rows=0'),
        (null, $q$update public.app_config set value = ((select count(*) from public.profiles) + 1)::text where key = 'max_users'$q$, 'ok'),
        ('After the limit is raised by 1, the same invitation works',
         $q$insert into auth.users (id, email, raw_app_meta_data) values (gen_random_uuid(), 'lim3@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$, 'ok'),

        -- ===== optional hook function (clear error messages) and leftovers =====
        (null, $q$update public.app_config set value = ((select count(*) from public.profiles) + 50)::text where key = 'max_users'$q$, 'ok'),
        ('Hook function: uninvited email gets an error with http_code 403',
         $q$select 1 where (public.hook_before_user_created('{"user":{"email":"nobody@thecount-test.invalid"}}'::jsonb) -> 'error' ->> 'http_code') = '403'$q$, 'rows=1'),
        ('Hook function: invited email gets an empty (allow) answer',
         $q$select 1 where public.hook_before_user_created('{"user":{"email":"owner@thecount-test.invalid"}}'::jsonb) = '{}'::jsonb$q$, 'rows=1'),
        ('Old draft table pending_registrations does not exist',
         $q$select 1 where to_regclass('public.pending_registrations') is not null$q$, 'rows=0'),
        ('Sign-up guard trigger is installed on auth.users',
         $q$select 1 from pg_trigger where tgrelid = 'auth.users'::regclass and tgname = 'trg_thecount_guard_new_user' and not tgisinternal$q$, 'rows=1')
      ) as v(name, stmt, expect)
    loop
      i := i + 1;
      begin
        execute chk.stmt;
        get diagnostics n = row_count;
        if chk.expect = 'ok' then
          ok := true;  detail_t := 'ok';
        elsif chk.expect like 'rows=%' then
          ok := (n = substr(chk.expect, 6)::bigint);  detail_t := n || ' row(s)';
        else
          ok := false; detail_t := 'was accepted but should have been rejected';
        end if;
      exception when others then
        if chk.expect like 'err=%' then
          states := split_part(split_part(chk.expect, '=', 2), '~', 1);
          msg    := nullif(split_part(chk.expect, '~', 2), '');
          ok := (sqlstate = any (string_to_array(states, '|'))) and (msg is null or sqlerrm like '%' || msg || '%');
          detail_t := 'rejected: ' || left(sqlerrm, 90);
        else
          ok := false; detail_t := 'unexpected error ' || sqlstate || ': ' || left(sqlerrm, 120);
        end if;
      end;

      if chk.name is null then
        if not ok then
          res := res || jsonb_build_object('n', i, 'name', 'SETUP step ' || i, 'result', 'FAIL', 'detail', detail_t);
          n_fail := n_fail + 1;
          exit;  -- later checks would be meaningless
        end if;
      else
        k := k + 1;
        res := res || jsonb_build_object('n', k, 'name', chk.name, 'result', case when ok then 'PASS' else 'FAIL' end, 'detail', detail_t);
        if ok then n_pass := n_pass + 1; else n_fail := n_fail + 1; end if;
      end if;
    end loop;

    raise exception 'rollback everything' using errcode = 'TC999';
  exception
    when sqlstate 'TC999' then null;      -- the normal, intended rollback
    when others then
      res := res || jsonb_build_object('n', 998, 'name', 'TEST CRASHED', 'result', 'FAIL', 'detail', sqlstate || ': ' || left(sqlerrm, 150));
      n_fail := n_fail + 1;
  end;

  return query
    select (r ->> 'n')::int, r ->> 'name', r ->> 'result', r ->> 'detail' from unnest(res) as r
    union all
    select 999, 'SUMMARY (all test data was rolled back)',
           case when n_fail = 0 and n_pass > 0 then 'PASS' else 'FAIL' end,
           'passed ' || n_pass || ', failed ' || n_fail
    order by 1;
end;
$fn$;

select * from pg_temp.thecount_guard_test() order by step;
