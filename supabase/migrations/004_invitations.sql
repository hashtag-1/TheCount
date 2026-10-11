-- 004_invitations.sql
-- Invitations, admins, app_config (server-managed)

-- Ensure app_config has max_users
insert into app_config (key, value)
values ('max_users', '120')
on conflict (key) do nothing;

-- Optional helper to count active (non-deleted) profiles/users accounted for
create or replace function thecount_active_user_count()
returns bigint as $$
  select count(*) from profiles where deleted_at is null;
$$ language sql security definer set search_path = public;

-- Invitations helper views/functions can be used by admins via dashboard;
-- no policies for ordinary users - RLS enabled but no grants/policies.
-- Ensure partial unique index exists (if not present)
do $$
begin
  if not exists (
    select 1 from pg_indexes where schemaname='public' and indexname='idx_invitations_active_email'
  ) then
    create unique index idx_invitations_active_email on invitations(email) where status = 'active';
  end if;
end $$;
