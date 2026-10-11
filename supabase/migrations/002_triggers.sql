-- 002_triggers.sql
-- The server, not the client, controls id, user_id, created_at, updated_at and version.
-- Run ONCE, after 001.

-- SECURITY DEFINER so nextval() works without giving app users any access to the sequence.
create or replace function public.thecount_stamp_row()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    new.created_at := now();
    new.updated_at := now();
  else
    if new.user_id is distinct from old.user_id then
      raise exception 'user_id cannot be changed';
    end if;
    new.id         := old.id;
    new.created_at := old.created_at;
    new.updated_at := now();
  end if;
  new.version := nextval('public.thecount_version_seq');
  return new;
end;
$$;

create or replace function public.thecount_touch_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at := now();
  new.id         := old.id;
  new.created_at := old.created_at;
  return new;
end;
$$;

drop trigger if exists trg_daily_strokes_stamp on public.daily_strokes;
create trigger trg_daily_strokes_stamp
  before insert or update on public.daily_strokes
  for each row execute function public.thecount_stamp_row();

drop trigger if exists trg_payments_stamp on public.payments;
create trigger trg_payments_stamp
  before insert or update on public.payments
  for each row execute function public.thecount_stamp_row();

drop trigger if exists trg_profiles_touch on public.profiles;
create trigger trg_profiles_touch
  before update on public.profiles
  for each row execute function public.thecount_touch_updated_at();
