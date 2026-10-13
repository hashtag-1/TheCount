-- 006_sync.sql  (Task 7A-1)
-- Push functions for offline sync. Run ONCE, after 001-005.
--
-- Design:
--  * SECURITY INVOKER: RLS from 003 still applies. A user can only ever see/change their own
--    rows, and only with an active profile.
--  * The user is ALWAYS auth.uid(). There is no user-id parameter, so a client cannot write
--    for someone else.
--  * The server, not the client, assigns `version` (trigger 002). The client sends the version
--    it last saw (p_base_version). If it does not match, nothing is overwritten: the function
--    returns status 'conflict' plus the server's current values (decision D9).
--  * Retrying the same push is safe (idempotent): if the server already holds exactly what is
--    being pushed, it answers 'ok' with the current version.
--  * Deletes are soft (deleted_at). A delete of a never-synced row stores a tombstone.
--
-- Return value (jsonb):
--   {"status":"ok","version":N}
--   {"status":"conflict","reason":"...","version":N, "server_...": ...}
-- Bad data (negative strokes, date before 2026-09-02, amount <= 0, ...) raises a normal
-- Postgres error from the table constraints.

create or replace function public.thecount_push_stroke(
  p_id           uuid,
  p_work_date    date,
  p_stroke_count bigint,
  p_deleted      boolean,
  p_base_version bigint
)
returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_del boolean := coalesce(p_deleted, false);
  r     public.daily_strokes%rowtype;
begin
  if v_uid is null then
    raise exception 'THECOUNT_NOT_SIGNED_IN' using errcode = '28000';
  end if;
  if p_id is null or p_work_date is null or p_stroke_count is null then
    raise exception 'THECOUNT_BAD_INPUT: id, work_date and stroke_count are required' using errcode = '22023';
  end if;

  -- One row per (user, date), soft-deleted rows included. RLS limits this to the caller's rows.
  select * into r
  from public.daily_strokes
  where user_id = v_uid and work_date = p_work_date
  for update;

  if not found then
    insert into public.daily_strokes (id, user_id, work_date, stroke_count, deleted_at)
    values (p_id, v_uid, p_work_date, p_stroke_count, case when v_del then now() else null end)
    on conflict do nothing
    returning * into r;

    if not found then
      -- The id is already used by another row (never reveal whose), or a concurrent insert won.
      return jsonb_build_object('status', 'conflict', 'reason', 'insert_blocked');
    end if;
    return jsonb_build_object('status', 'ok', 'version', r.version);
  end if;

  -- The date already exists on the server under a DIFFERENT id: never duplicate, never overwrite.
  if r.id <> p_id then
    return jsonb_build_object(
      'status', 'conflict', 'reason', 'date_exists_other_id',
      'version', r.version,
      'server_id', r.id,
      'server_stroke_count', r.stroke_count,
      'server_deleted', r.deleted_at is not null);
  end if;

  -- Same id. Already identical on the server? Then a retry after a lost response: succeed.
  if (r.deleted_at is not null) = v_del
     and (v_del or r.stroke_count = p_stroke_count) then
    return jsonb_build_object('status', 'ok', 'version', r.version);
  end if;

  -- Optimistic concurrency: only the holder of the current version may change it.
  if p_base_version is not null and p_base_version = r.version then
    update public.daily_strokes
       set stroke_count = p_stroke_count,
           deleted_at   = case when v_del then coalesce(r.deleted_at, now()) else null end
     where id = r.id
    returning * into r;
    return jsonb_build_object('status', 'ok', 'version', r.version);
  end if;

  return jsonb_build_object(
    'status', 'conflict', 'reason', 'version_mismatch',
    'version', r.version,
    'server_id', r.id,
    'server_stroke_count', r.stroke_count,
    'server_deleted', r.deleted_at is not null);
end;
$$;

create or replace function public.thecount_push_payment(
  p_id           uuid,
  p_payment_date date,
  p_amount       numeric,
  p_note         text,
  p_deleted      boolean,
  p_base_version bigint
)
returns jsonb
language plpgsql
security invoker
set search_path = ''
as $$
declare
  v_uid uuid := auth.uid();
  v_del boolean := coalesce(p_deleted, false);
  r     public.payments%rowtype;
begin
  if v_uid is null then
    raise exception 'THECOUNT_NOT_SIGNED_IN' using errcode = '28000';
  end if;
  if p_id is null or p_payment_date is null or p_amount is null then
    raise exception 'THECOUNT_BAD_INPUT: id, payment_date and amount are required' using errcode = '22023';
  end if;

  -- RLS limits this to the caller's own rows, so another user's id simply is not found.
  select * into r
  from public.payments
  where id = p_id
  for update;

  if not found then
    insert into public.payments (id, user_id, payment_date, amount, note, deleted_at)
    values (p_id, v_uid, p_payment_date, p_amount, p_note, case when v_del then now() else null end)
    on conflict do nothing
    returning * into r;

    if not found then
      return jsonb_build_object('status', 'conflict', 'reason', 'insert_blocked');
    end if;
    return jsonb_build_object('status', 'ok', 'version', r.version);
  end if;

  -- Identical already (retry after a lost response)?
  if (r.deleted_at is not null) = v_del
     and (v_del or (r.payment_date = p_payment_date
                    and r.amount = p_amount
                    and r.note is not distinct from p_note)) then
    return jsonb_build_object('status', 'ok', 'version', r.version);
  end if;

  if p_base_version is not null and p_base_version = r.version then
    update public.payments
       set payment_date = p_payment_date,
           amount       = p_amount,
           note         = p_note,
           deleted_at   = case when v_del then coalesce(r.deleted_at, now()) else null end
     where id = r.id
    returning * into r;
    return jsonb_build_object('status', 'ok', 'version', r.version);
  end if;

  return jsonb_build_object(
    'status', 'conflict', 'reason', 'version_mismatch',
    'version', r.version,
    'server_payment_date', r.payment_date,
    'server_amount', r.amount::text,
    'server_note', r.note,
    'server_deleted', r.deleted_at is not null);
end;
$$;

-- Only signed-in app users may call these (same pattern as 003).
revoke all on function public.thecount_push_stroke(uuid, date, bigint, boolean, bigint)
  from public, anon;
revoke all on function public.thecount_push_payment(uuid, date, numeric, text, boolean, bigint)
  from public, anon;
grant execute on function public.thecount_push_stroke(uuid, date, bigint, boolean, bigint)
  to authenticated;
grant execute on function public.thecount_push_payment(uuid, date, numeric, text, boolean, bigint)
  to authenticated;

-- Check: both functions must be listed, security_definer = false.
select p.proname, p.prosecdef as security_definer
from pg_proc p join pg_namespace n on n.oid = p.pronamespace
where n.nspname = 'public' and p.proname in ('thecount_push_stroke', 'thecount_push_payment');
