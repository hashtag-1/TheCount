-- 003_rls.sql
-- enable RLS and basic helper + policies

alter table profiles enable row level security;
alter table daily_strokes enable row level security;
alter table payments enable row level security;
alter table invitations enable row level security;
alter table admins enable row level security;
alter table app_config enable row level security;

-- helper: user has active profile
create or replace function thecount_has_active_profile(uid uuid)
returns boolean as $$
  select exists (select 1 from profiles p where p.id = uid and p.deleted_at is null);
$$ language sql security definer set search_path = public;

-- profiles: users can read their own
create policy "profiles_select_own" on profiles
  for select using (auth.uid() = id);
create policy "profiles_insert_own" on profiles
  for insert with check (auth.uid() = id);
create policy "profiles_update_own" on profiles
  for update using (auth.uid() = id) with check (auth.uid() = id);
create policy "profiles_delete_own" on profiles
  for delete using (auth.uid() = id);

-- daily_strokes: own rows only, require active profile
create policy "daily_strokes_select_own" on daily_strokes
  for select using (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));
create policy "daily_strokes_insert_own" on daily_strokes
  for insert with check (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));
create policy "daily_strokes_update_own" on daily_strokes
  for update using (auth.uid() = user_id and thecount_has_active_profile(auth.uid())) with check (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));
create policy "daily_strokes_delete_own" on daily_strokes
  for delete using (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));

-- payments
create policy "payments_select_own" on payments
  for select using (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));
create policy "payments_insert_own" on payments
  for insert with check (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));
create policy "payments_update_own" on payments
  for update using (auth.uid() = user_id and thecount_has_active_profile(auth.uid())) with check (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));
create policy "payments_delete_own" on payments
  for delete using (auth.uid() = user_id and thecount_has_active_profile(auth.uid()));

-- no policies for invitations/admins/app_config for ordinary users (RLS enabled; no grants by default)
