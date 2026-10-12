-- rls_isolation.sql  (Task 5C, reviewed rewrite)
-- Proves, on YOUR Supabase project, that users cannot see or change each other's data,
-- that clients cannot forge server fields or touch protected tables, and that anonymous,
-- uninvited and suspended users get nothing.
--
-- HOW TO RUN: Supabase -> SQL Editor -> New query -> paste this WHOLE file -> Run.
-- The result grid at the bottom lists every check as PASS or FAIL, and the last row is SUMMARY.
--
-- SAFETY: everything (4 fake test users, their rows, a temporary max_users change) happens inside
-- a block that is ALWAYS rolled back, even if a check crashes. Real users and real data are never
-- modified. Test emails end in @thecount-test.invalid.
-- Afterwards run:  select * from public.app_config;   max_users must still be your value (120).

create or replace function pg_temp.thecount_rls_test()
returns table (step int, check_name text, result text, detail text)
language plpgsql
as $fn$
declare
  v_a uuid := gen_random_uuid();  v_b uuid := gen_random_uuid();
  v_c uuid := gen_random_uuid();  v_d uuid := gen_random_uuid();
  v_ghost uuid := gen_random_uuid();
  s_a uuid := gen_random_uuid();  s_b uuid := gen_random_uuid();  s_d uuid := gen_random_uuid();
  s_f uuid := gen_random_uuid();  s_n uuid := gen_random_uuid();
  p_a uuid := gen_random_uuid();  p_b uuid := gen_random_uuid();
  chk      record;
  q        text;
  uid      uuid;
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
        -- ===== setup as admin (silent: name is null; must succeed) =====
        (null::text, 'admin', $q$update public.app_config set value = ((select count(*) from public.profiles) + 20)::text where key = 'max_users'$q$, 'ok'),
        (null, 'admin', $q$insert into public.invitations (email) values
            ('rls-a@thecount-test.invalid'), ('rls-b@thecount-test.invalid'),
            ('rls-c@thecount-test.invalid'), ('rls-d@thecount-test.invalid')$q$, 'ok'),
        (null, 'admin', $q$insert into auth.users (id, email, raw_app_meta_data) values
            ('{A}', 'rls-a@thecount-test.invalid', '{"provider":"google"}'::jsonb),
            ('{B}', 'rls-b@thecount-test.invalid', '{"provider":"google"}'::jsonb),
            ('{C}', 'rls-c@thecount-test.invalid', '{"provider":"google"}'::jsonb),
            ('{D}', 'rls-d@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$, 'ok'),
        ('Setup: the guard created active profiles for the test users A and B',
         'admin', $q$select 1 from public.profiles where id in ('{A}', '{B}') and status = 'active'$q$, 'rows=2'),
        (null, 'admin', $q$insert into public.daily_strokes (id, user_id, work_date, stroke_count) values
            ('{SA}', '{A}', '2026-09-02', 5), ('{SB}', '{B}', '2026-09-03', 3), ('{SD}', '{D}', '2026-09-02', 2)$q$, 'ok'),
        (null, 'admin', $q$insert into public.payments (id, user_id, payment_date, amount, note) values
            ('{PA}', '{A}', '2026-09-02', 20.00, 'a'), ('{PB}', '{B}', '2026-09-03', 10.00, 'b')$q$, 'ok'),
        (null, 'admin', $q$delete from public.profiles where id = '{C}'$q$, 'ok'),                       -- C = signed in, no profile
        (null, 'admin', $q$update public.profiles set status = 'suspended' where id = '{D}'$q$, 'ok'),  -- D = suspended

        -- ===== positive controls: users CAN use their own data =====
        ('A sees exactly their own stroke (not B''s or D''s)', 'A', $q$select 1 from public.daily_strokes$q$, 'rows=1'),
        ('A sees exactly their own payment', 'A', $q$select 1 from public.payments$q$, 'rows=1'),
        ('A sees exactly their own profile', 'A', $q$select 1 from public.profiles$q$, 'rows=1'),
        ('B sees exactly their own stroke', 'B', $q$select 1 from public.daily_strokes$q$, 'rows=1'),

        -- ===== A cannot reach B's data =====
        ('A cannot read B''s stroke even by guessing its id', 'A', $q$select 1 from public.daily_strokes where id = '{SB}'$q$, 'rows=0'),
        ('A cannot read B''s payment even by guessing its id', 'A', $q$select 1 from public.payments where id = '{PB}'$q$, 'rows=0'),
        ('A cannot read B''s profile', 'A', $q$select 1 from public.profiles where id = '{B}'$q$, 'rows=0'),
        ('A cannot insert a stroke for B', 'A', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{B}', '2026-09-10', 1)$q$, 'err=42501'),
        ('A cannot insert a payment for B', 'A', $q$insert into public.payments (user_id, payment_date, amount) values ('{B}', '2026-09-10', 5)$q$, 'err=42501'),
        ('A cannot update B''s stroke (0 rows changed)', 'A', $q$update public.daily_strokes set stroke_count = 99 where id = '{SB}'$q$, 'rows=0'),
        ('A cannot update B''s payment (0 rows changed)', 'A', $q$update public.payments set amount = 99 where id = '{PB}'$q$, 'rows=0'),
        ('A cannot hard-delete B''s stroke', 'A', $q$delete from public.daily_strokes where id = '{SB}'$q$, 'err=42501'),
        ('A cannot hard-delete their own stroke either (soft delete only)', 'A', $q$delete from public.daily_strokes where id = '{SA}'$q$, 'err=42501'),
        ('A cannot hard-delete payments', 'A', $q$delete from public.payments where id = '{PA}'$q$, 'err=42501'),
        ('A cannot move their stroke to B (user_id is locked)', 'A', $q$update public.daily_strokes set user_id = '{B}' where id = '{SA}'$q$, 'err=P0001|42501'),
        ('...and A''s stroke still belongs to A', 'admin', $q$select 1 from public.daily_strokes where id = '{SA}' and user_id = '{A}'$q$, 'rows=1'),
        ('...and B''s data is untouched', 'admin', $q$select 1 from public.daily_strokes where id = '{SB}' and stroke_count = 3 union all select 1 from public.payments where id = '{PB}' and amount = 10.00$q$, 'rows=2'),

        -- ===== the server, not the client, controls version and timestamps =====
        ('A can insert their own stroke while forging version and created_at', 'A',
         $q$insert into public.daily_strokes (id, user_id, work_date, stroke_count, version, created_at) values ('{SF}', '{A}', '2026-09-05', 7, 999999999, '2000-01-01')$q$, 'ok'),
        ('...the forged version and created_at were overwritten by the server', 'admin',
         $q$select 1 from public.daily_strokes where id = '{SF}' and version <> 999999999 and created_at > '2020-01-01'$q$, 'rows=1'),
        ('A can update their own stroke while forging version', 'A', $q$update public.daily_strokes set stroke_count = 8, version = 1 where id = '{SF}'$q$, 'rows=1'),
        ('...the server assigned a new version anyway', 'admin', $q$select 1 from public.daily_strokes where id = '{SF}' and stroke_count = 8 and version <> 1$q$, 'rows=1'),
        ('A can soft-delete their own stroke (set deleted_at)', 'A', $q$update public.daily_strokes set deleted_at = now() where id = '{SA}'$q$, 'rows=1'),

        -- ===== server-side validation (cannot be bypassed from the app) =====
        ('Server rejects a stroke date before 2026-09-02', 'A', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{A}', '2026-09-01', 1)$q$, 'err=23514'),
        ('Server rejects a negative stroke count', 'A', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{A}', '2026-09-20', -5)$q$, 'err=23514'),
        ('Server rejects a payment of 0', 'A', $q$insert into public.payments (user_id, payment_date, amount) values ('{A}', '2026-09-20', 0)$q$, 'err=23514'),
        ('Server rejects a payment dated before 2026-09-02', 'A', $q$insert into public.payments (user_id, payment_date, amount) values ('{A}', '2026-09-01', 5)$q$, 'err=23514'),
        ('Server rejects a second stroke row for the same day', 'A', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{A}', '2026-09-05', 1)$q$, 'err=23505'),

        -- ===== profiles are server-controlled =====
        ('An uninvited signed-in user cannot create their own profile to get in', 'GHOST', $q$insert into public.profiles (id, email, status) values ('{GHOST}', 'ghost@thecount-test.invalid', 'active')$q$, 'err=42501'),
        ('A user whose profile is missing cannot re-create it', 'C', $q$insert into public.profiles (id, email, status) values ('{C}', 'rls-c@thecount-test.invalid', 'active')$q$, 'err=42501'),
        ('Client cannot create a profile for someone else', 'A', $q$insert into public.profiles (id, email, status) values ('{GHOST}', 'ghost2@thecount-test.invalid', 'active')$q$, 'err=42501'),
        ('Client cannot edit their own profile (e.g. status)', 'A', $q$update public.profiles set status = 'suspended' where id = '{A}'$q$, 'err=42501'),
        ('Client cannot delete their profile', 'A', $q$delete from public.profiles where id = '{A}'$q$, 'err=42501'),

        -- ===== protected tables and functions =====
        ('Client cannot read invitations', 'A', $q$select 1 from public.invitations$q$, 'err=42501'),
        ('Client cannot create an invitation', 'A', $q$insert into public.invitations (email) values ('self-invite@thecount-test.invalid')$q$, 'err=42501'),
        ('Client cannot read admins', 'A', $q$select 1 from public.admins$q$, 'err=42501'),
        ('Client cannot read app_config', 'A', $q$select 1 from public.app_config$q$, 'err=42501'),
        ('Client cannot change the user limit', 'A', $q$update public.app_config set value = '99999' where key = 'max_users'$q$, 'err=42501'),
        ('Client cannot call the user-count function', 'A', $q$select public.thecount_user_count()$q$, 'err=42501'),
        ('Client cannot call the sign-up hook function', 'A', $q$select public.hook_before_user_created('{}'::jsonb)$q$, 'err=42501'),

        -- ===== anonymous (no login at all) =====
        ('Anonymous cannot read strokes', 'anon', $q$select 1 from public.daily_strokes$q$, 'err=42501'),
        ('Anonymous cannot read payments', 'anon', $q$select 1 from public.payments$q$, 'err=42501'),
        ('Anonymous cannot read profiles', 'anon', $q$select 1 from public.profiles$q$, 'err=42501'),
        ('Anonymous cannot read invitations', 'anon', $q$select 1 from public.invitations$q$, 'err=42501'),
        ('Anonymous cannot insert a stroke', 'anon', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{A}', '2026-09-22', 1)$q$, 'err=42501'),

        -- ===== signed in but not allowed: no profile / suspended / unknown =====
        ('Signed-in user with NO profile sees no strokes', 'C', $q$select 1 from public.daily_strokes$q$, 'rows=0'),
        ('Signed-in user with NO profile cannot insert a stroke', 'C', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{C}', '2026-09-22', 1)$q$, 'err=42501'),
        ('Suspended user sees none of their own strokes', 'D', $q$select 1 from public.daily_strokes$q$, 'rows=0'),
        ('Suspended user cannot insert a stroke', 'D', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{D}', '2026-09-23', 1)$q$, 'err=42501'),
        ('Suspended user cannot update their stroke (0 rows)', 'D', $q$update public.daily_strokes set stroke_count = 50 where id = '{SD}'$q$, 'rows=0'),
        ('Login for an id with no account at all sees nothing', 'GHOST', $q$select 1 from public.daily_strokes$q$, 'rows=0'),
        ('Login for an id with no account cannot insert', 'GHOST', $q$insert into public.daily_strokes (user_id, work_date, stroke_count) values ('{GHOST}', '2026-09-22', 1)$q$, 'err=42501')
      ) as v(name, who, stmt, expect)
    loop
      i := i + 1;
      q := replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(chk.stmt,
             '{GHOST}', v_ghost::text), '{SA}', s_a::text), '{SB}', s_b::text), '{SD}', s_d::text), '{SF}', s_f::text),
             '{PA}', p_a::text), '{PB}', p_b::text), '{A}', v_a::text), '{B}', v_b::text), '{C}', v_c::text), '{D}', v_d::text), '{SN}', s_n::text);

      -- choose who is running the statement
      execute 'reset role';
      if chk.who = 'anon' then
        execute 'set local role anon';
        perform set_config('request.jwt.claims', '', true);
      elsif chk.who <> 'admin' then
        uid := case chk.who when 'A' then v_a when 'B' then v_b when 'C' then v_c when 'D' then v_d else v_ghost end;
        execute 'set local role authenticated';
        perform set_config('request.jwt.claims', json_build_object('sub', uid::text, 'role', 'authenticated')::text, true);
      end if;

      begin
        execute q;
        get diagnostics n = row_count;
        if chk.expect = 'ok' then
          ok := true;  detail_t := 'ok';
        elsif chk.expect like 'rows=%' then
          ok := (n = substr(chk.expect, 6)::bigint);  detail_t := n || ' row(s)';
        else
          ok := false; detail_t := 'was allowed but should have been blocked';
        end if;
      exception when others then
        if chk.expect like 'err=%' then
          states := split_part(split_part(chk.expect, '=', 2), '~', 1);
          msg    := nullif(split_part(chk.expect, '~', 2), '');
          ok := (sqlstate = any (string_to_array(states, '|'))) and (msg is null or sqlerrm like '%' || msg || '%');
          detail_t := 'blocked: ' || left(sqlerrm, 90);
        else
          ok := false; detail_t := 'unexpected error ' || sqlstate || ': ' || left(sqlerrm, 120);
        end if;
      end;
      execute 'reset role';

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

select * from pg_temp.thecount_rls_test() order by step;
