-- sync_push.sql  (Task 7A-1)
-- Tests the two push functions from 006_sync.sql on YOUR Supabase project.
-- Same safety model as rls_isolation.sql: everything (2 fake users, their rows, a temporary
-- max_users change) happens in a block that is ALWAYS rolled back. Real data is never touched.
-- Test emails end in @thecount-test.invalid.
--
-- HOW TO RUN: Supabase -> SQL Editor -> New query -> paste this WHOLE file -> Run.
-- The result grid lists every check as PASS or FAIL; the last row is SUMMARY.
-- Afterwards run:  select * from public.app_config;   max_users must still be 120.

create or replace function pg_temp.thecount_sync_test()
returns table (step int, check_name text, result text, detail text)
language plpgsql
as $fn$
declare
  v_a uuid := gen_random_uuid();  v_b uuid := gen_random_uuid();
  s1 uuid := gen_random_uuid();   s2 uuid := gen_random_uuid();
  s3 uuid := gen_random_uuid();   s4 uuid := gen_random_uuid();
  p1 uuid := gen_random_uuid();   p2 uuid := gen_random_uuid();
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
            ('sync-a@thecount-test.invalid'), ('sync-b@thecount-test.invalid')$q$, 'ok'),
        (null, 'admin', $q$insert into auth.users (id, email, raw_app_meta_data) values
            ('{A}', 'sync-a@thecount-test.invalid', '{"provider":"google"}'::jsonb),
            ('{B}', 'sync-b@thecount-test.invalid', '{"provider":"google"}'::jsonb)$q$, 'ok'),
        ('Setup: test users A and B have active profiles',
         'admin', $q$select 1 from public.profiles where id in ('{A}', '{B}') and status = 'active'$q$, 'rows=2'),

        -- ===== strokes: insert, retry, edit =====
        ('Stroke: pushing a new row returns ok', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-02', 100, false, null)->>'status') = 'ok'$q$, 'rows=1'),
        ('Stroke: the server now holds it with a server-assigned version', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S1}' and user_id = '{A}' and stroke_count = 100 and version > 0$q$, 'rows=1'),
        ('Stroke: pushing the SAME thing again (lost-response retry) returns ok', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-02', 100, false, null)->>'status') = 'ok'$q$, 'rows=1'),
        ('Stroke: ...and there is still exactly one row for that date', 'admin',
         $q$select 1 from public.daily_strokes where user_id = '{A}' and work_date = '2026-09-02'$q$, 'rows=1'),
        ('Stroke: edit with the correct base version returns ok', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-02', 150, false,
              (select version from public.daily_strokes where id = '{S1}'))->>'status') = 'ok'$q$, 'rows=1'),
        ('Stroke: ...and the new count (150) is stored', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S1}' and stroke_count = 150$q$, 'rows=1'),
        ('Stroke: edit with a STALE base version returns conflict', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-02', 200, false,
              (select version - 1 from public.daily_strokes where id = '{S1}'))->>'status') = 'conflict'$q$, 'rows=1'),
        ('Stroke: ...and nothing was overwritten (still 150)', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S1}' and stroke_count = 150$q$, 'rows=1'),
        ('Stroke: edit with NO base version on an existing, different row returns conflict', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-02', 300, false, null)->>'status') = 'conflict'$q$, 'rows=1'),
        ('Stroke: same date under a DIFFERENT id returns conflict', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S2}', '2026-09-02', 5, false, null)->>'status') = 'conflict'$q$, 'rows=1'),
        ('Stroke: ...and no duplicate row was created', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S2}'$q$, 'rows=0'),

        -- ===== strokes: soft delete, revive, tombstone =====
        ('Stroke: soft delete with the correct base version returns ok', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-02', 150, true,
              (select version from public.daily_strokes where id = '{S1}'))->>'status') = 'ok'$q$, 'rows=1'),
        ('Stroke: ...the row is kept with deleted_at set (not removed)', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S1}' and deleted_at is not null$q$, 'rows=1'),
        ('Stroke: re-entering the deleted date revives the same row (same id)', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-02', 175, false,
              (select version from public.daily_strokes where id = '{S1}'))->>'status') = 'ok'$q$, 'rows=1'),
        ('Stroke: ...revived with the new count and no deleted_at', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S1}' and stroke_count = 175 and deleted_at is null$q$, 'rows=1'),
        ('Stroke: deleting a never-synced row stores a tombstone', 'A',
         $q$select 1 where (public.thecount_push_stroke('{S3}', '2026-09-03', 9, true, null)->>'status') = 'ok'$q$, 'rows=1'),
        ('Stroke: ...the tombstone exists with deleted_at set', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S3}' and deleted_at is not null$q$, 'rows=1'),

        -- ===== strokes: validation and isolation =====
        ('Stroke: server rejects a date before 2026-09-02', 'A',
         $q$select public.thecount_push_stroke('{S4}', '2026-09-01', 1, false, null)$q$, 'err=23514'),
        ('Stroke: server rejects a negative count', 'A',
         $q$select public.thecount_push_stroke('{S4}', '2026-09-04', -1, false, null)$q$, 'err=23514'),
        ('Stroke: user B cannot take over user A''s row by reusing its id', 'B',
         $q$select 1 where (public.thecount_push_stroke('{S1}', '2026-09-10', 1, false, null)->>'status') = 'conflict'$q$, 'rows=1'),
        ('Stroke: ...A''s row is untouched', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S1}' and user_id = '{A}' and stroke_count = 175$q$, 'rows=1'),
        ('Stroke: user B can use the same date as A for their own row', 'B',
         $q$select 1 where (public.thecount_push_stroke('{S4}', '2026-09-02', 7, false, null)->>'status') = 'ok'$q$, 'rows=1'),
        ('Stroke: ...it belongs to B, not A', 'admin',
         $q$select 1 from public.daily_strokes where id = '{S4}' and user_id = '{B}'$q$, 'rows=1'),
        ('Stroke: anonymous cannot call the function', 'anon',
         $q$select public.thecount_push_stroke('{S1}', '2026-09-02', 1, false, null)$q$, 'err=42501'),

        -- ===== payments =====
        ('Payment: pushing a new row returns ok', 'A',
         $q$select 1 where (public.thecount_push_payment('{P1}', '2026-09-02', 20.00, 'first', false, null)->>'status') = 'ok'$q$, 'rows=1'),
        ('Payment: the server holds it with the exact amount', 'admin',
         $q$select 1 from public.payments where id = '{P1}' and user_id = '{A}' and amount = 20.00 and version > 0$q$, 'rows=1'),
        ('Payment: pushing the SAME thing again returns ok, no duplicate', 'A',
         $q$select 1 where (public.thecount_push_payment('{P1}', '2026-09-02', 20.00, 'first', false, null)->>'status') = 'ok'$q$, 'rows=1'),
        ('Payment: ...still exactly one row', 'admin',
         $q$select 1 from public.payments where user_id = '{A}'$q$, 'rows=1'),
        ('Payment: edit with the correct base version returns ok', 'A',
         $q$select 1 where (public.thecount_push_payment('{P1}', '2026-09-02', 25.50, 'edited', false,
              (select version from public.payments where id = '{P1}'))->>'status') = 'ok'$q$, 'rows=1'),
        ('Payment: ...the new amount (25.50) is stored', 'admin',
         $q$select 1 from public.payments where id = '{P1}' and amount = 25.50 and note = 'edited'$q$, 'rows=1'),
        ('Payment: edit with a STALE base version returns conflict', 'A',
         $q$select 1 where (public.thecount_push_payment('{P1}', '2026-09-02', 99.00, 'stale', false,
              (select version - 1 from public.payments where id = '{P1}'))->>'status') = 'conflict'$q$, 'rows=1'),
        ('Payment: ...nothing was overwritten (still 25.50)', 'admin',
         $q$select 1 from public.payments where id = '{P1}' and amount = 25.50$q$, 'rows=1'),
        ('Payment: soft delete with the correct base version returns ok', 'A',
         $q$select 1 where (public.thecount_push_payment('{P1}', '2026-09-02', 25.50, 'edited', true,
              (select version from public.payments where id = '{P1}'))->>'status') = 'ok'$q$, 'rows=1'),
        ('Payment: ...the row is kept with deleted_at set', 'admin',
         $q$select 1 from public.payments where id = '{P1}' and deleted_at is not null$q$, 'rows=1'),
        ('Payment: server rejects an amount of 0', 'A',
         $q$select public.thecount_push_payment('{P2}', '2026-09-03', 0, null, false, null)$q$, 'err=23514'),
        ('Payment: server rejects a date before 2026-09-02', 'A',
         $q$select public.thecount_push_payment('{P2}', '2026-09-01', 5, null, false, null)$q$, 'err=23514'),
        ('Payment: user B cannot take over user A''s payment by reusing its id', 'B',
         $q$select 1 where (public.thecount_push_payment('{P1}', '2026-09-10', 1.00, 'hijack', false, null)->>'status') = 'conflict'$q$, 'rows=1'),
        ('Payment: ...A''s payment is untouched', 'admin',
         $q$select 1 from public.payments where id = '{P1}' and user_id = '{A}' and amount = 25.50$q$, 'rows=1'),
        ('Payment: anonymous cannot call the function', 'anon',
         $q$select public.thecount_push_payment('{P1}', '2026-09-02', 1, null, false, null)$q$, 'err=42501')
      ) as v(name, who, stmt, expect)
    loop
      i := i + 1;
      q := replace(replace(replace(replace(replace(replace(replace(replace(chk.stmt,
             '{S1}', s1::text), '{S2}', s2::text), '{S3}', s3::text), '{S4}', s4::text),
             '{P1}', p1::text), '{P2}', p2::text), '{A}', v_a::text), '{B}', v_b::text);

      -- choose who is running the statement
      execute 'reset role';
      if chk.who = 'anon' then
        execute 'set local role anon';
        perform set_config('request.jwt.claims', '', true);
      elsif chk.who <> 'admin' then
        uid := case chk.who when 'A' then v_a else v_b end;
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

select * from pg_temp.thecount_sync_test() order by step;
