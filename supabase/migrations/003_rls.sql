-- 003_rls.sql
-- Row Level Security + explicit grants. Run ONCE, after 002.
-- Rule: a signed-in user may touch ONLY their own rows, AND only with an active profile.
-- Profiles are created by the server in Task 5B (never by the client), so a user who
-- was not invited can never get an active profile.

alter table public.profiles      enable row level security;
alter table public.daily_strokes enable row level security;
alter table public.payments      enable row level security;
alter table public.invitations   enable row level security;
alter table public.admins        enable row level security;
alter table public.app_config    enable row level security;

-- Start from zero access for the two client roles, then grant the minimum.
revoke all on public.profiles, public.daily_strokes, public.payments,
              public.invitations, public.admins, public.app_config
  from anon, authenticated;

grant select         on public.profiles                       to authenticated;
grant select, insert, update on public.daily_strokes, public.payments to authenticated;
-- No DELETE grant: clients soft-delete by setting deleted_at (UPDATE).
-- Nothing at all is granted on invitations, admins, app_config.

-- Helper: does the caller have an active profile? SECURITY DEFINER so it can read profiles.
create or replace function public.thecount_is_active_user()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid()) and p.status = 'active'
  );
$$;
revoke all on function public.thecount_is_active_user() from public, anon;
grant execute on function public.thecount_is_active_user() to authenticated;

-- profiles: read your own row only. No client insert/update/delete.
create policy profiles_select_own on public.profiles
  for select to authenticated
  using (id = (select auth.uid()));

-- daily_strokes
create policy daily_strokes_select_own on public.daily_strokes
  for select to authenticated
  using (user_id = (select auth.uid()) and public.thecount_is_active_user());
create policy daily_strokes_insert_own on public.daily_strokes
  for insert to authenticated
  with check (user_id = (select auth.uid()) and public.thecount_is_active_user());
create policy daily_strokes_update_own on public.daily_strokes
  for update to authenticated
  using (user_id = (select auth.uid()) and public.thecount_is_active_user())
  with check (user_id = (select auth.uid()) and public.thecount_is_active_user());

-- payments
create policy payments_select_own on public.payments
  for select to authenticated
  using (user_id = (select auth.uid()) and public.thecount_is_active_user());
create policy payments_insert_own on public.payments
  for insert to authenticated
  with check (user_id = (select auth.uid()) and public.thecount_is_active_user());
create policy payments_update_own on public.payments
  for update to authenticated
  using (user_id = (select auth.uid()) and public.thecount_is_active_user())
  with check (user_id = (select auth.uid()) and public.thecount_is_active_user());

-- invitations, admins, app_config: RLS on, NO policies, NO grants = invisible to clients.

-- Check: every table below must show rls_enabled = true.
select c.relname as table_name, c.relrowsecurity as rls_enabled
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public' and c.relkind = 'r'
order by c.relname;
