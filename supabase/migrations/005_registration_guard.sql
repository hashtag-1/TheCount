-- 005_registration_guard.sql
-- Server-side registration guard. Run ONCE, after 004.
--
-- HOW IT WORKS
-- A trigger on auth.users runs INSIDE the same database transaction that creates the account.
-- In one locked transaction it checks, in this order:
--   1. the sign-in is a Google sign-in
--   2. the email matches an ACTIVE, unexpired invitation
--   3. the number of accounts is below app_config.max_users
-- If any check fails it raises an exception, the whole transaction rolls back, and NO account
-- (and no half-account) exists. If all pass it marks the invitation used and creates the
-- profile (status active) in that same transaction. A global advisory lock serialises
-- sign-ups, so two people can never both take the last slot.
-- Clients can never create profiles: only this function can.

-- Remove objects from an earlier draft of this task (safe if absent).
drop trigger  if exists trg_auth_users_create_profile on auth.users;
drop function if exists public.thecount_create_profile_on_signup();
drop table    if exists public.pending_registrations cascade;

create or replace function public.thecount_guard_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_email    text := lower(btrim(coalesce(new.email, '')));
  v_max      integer;
  v_count    bigint;
  v_invite   uuid;
begin
  -- one sign-up at a time (released automatically at commit/rollback)
  perform pg_advisory_xact_lock(hashtext('thecount_registration'));

  if coalesce(new.raw_app_meta_data ->> 'provider', '') <> 'google' then
    raise exception 'THECOUNT_DENIED: only Google sign-in is allowed' using errcode = 'P0001';
  end if;

  if v_email = '' then
    raise exception 'THECOUNT_NOT_INVITED: no email address' using errcode = 'P0001';
  end if;

  select i.id into v_invite
  from public.invitations i
  where i.email = v_email
    and i.status = 'active'
    and (i.expires_at is null or i.expires_at > now())
  for update;

  if v_invite is null then
    raise exception 'THECOUNT_NOT_INVITED: this email has no active invitation' using errcode = 'P0001';
  end if;

  select value::integer into v_max from public.app_config where key = 'max_users';
  if v_max is null then
    raise exception 'THECOUNT_DENIED: max_users is not configured' using errcode = 'P0001';
  end if;

  select count(*) into v_count from public.profiles;
  if v_count >= v_max then
    raise exception 'THECOUNT_LIMIT_REACHED: the user limit has been reached' using errcode = 'P0001';
  end if;

  update public.invitations
     set status = 'used', used_at = now(), used_by = new.id
   where id = v_invite;

  insert into public.profiles (id, email, display_name, status)
  values (new.id, v_email, new.raw_user_meta_data ->> 'full_name', 'active');

  return new;
end;
$$;

revoke all on function public.thecount_guard_new_user() from public, anon, authenticated;

drop trigger if exists trg_thecount_guard_new_user on auth.users;
create trigger trg_thecount_guard_new_user
  after insert on auth.users
  for each row execute function public.thecount_guard_new_user();

-- OPTIONAL: "Before User Created" hook = a clearer error message for the app.
-- It only READS and returns an error; the trigger above is the real enforcement and works
-- even if you never enable the hook. Enable it in the dashboard:
-- Authentication -> Hooks -> Before User Created -> Postgres function -> hook_before_user_created
create or replace function public.hook_before_user_created(event jsonb)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_email text := lower(btrim(coalesce(event -> 'user' ->> 'email', '')));
  v_max   integer;
begin
  if not exists (
    select 1 from public.invitations i
    where i.email = v_email and i.status = 'active'
      and (i.expires_at is null or i.expires_at > now())
  ) then
    return jsonb_build_object('error', jsonb_build_object(
      'http_code', 403,
      'message', 'THECOUNT_NOT_INVITED: this email has no active invitation'));
  end if;

  select value::integer into v_max from public.app_config where key = 'max_users';
  if v_max is null or (select count(*) from public.profiles) >= v_max then
    return jsonb_build_object('error', jsonb_build_object(
      'http_code', 403,
      'message', 'THECOUNT_LIMIT_REACHED: the user limit has been reached'));
  end if;

  return '{}'::jsonb;
end;
$$;

grant usage on schema public to supabase_auth_admin;
grant execute on function public.hook_before_user_created(jsonb) to supabase_auth_admin;
revoke execute on function public.hook_before_user_created(jsonb) from public, anon, authenticated;
