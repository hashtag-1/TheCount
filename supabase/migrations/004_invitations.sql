-- 004_invitations.sql
-- Invitation-only registration: config + helper functions. Run ONCE, after 003.
-- (invitations, admins and app_config tables, RLS and the one-active-invitation-per-email
--  index already exist from 001 and 003. Clients can never read or write them.)

-- Clean up anything an earlier draft of this task may have created (safe if absent).
drop index    if exists public.idx_invitations_active_email;
drop function if exists public.thecount_active_user_count();

-- Maximum accounts. Change it later with: update public.app_config set value='120' where key='max_users';
insert into public.app_config (key, value)
values ('max_users', '120')
on conflict (key) do nothing;

-- Dashboard helper: how many accounts hold a slot (every profile, active or suspended).
create or replace function public.thecount_user_count()
returns bigint
language sql
stable
security definer
set search_path = ''
as $$
  select count(*) from public.profiles;
$$;

-- Supabase exposes public functions to the app by default. Lock these down.
revoke all on function public.thecount_user_count() from public, anon, authenticated;
