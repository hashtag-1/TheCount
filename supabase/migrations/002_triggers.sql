-- 002_triggers.sql
-- updated_at bump and prevent user_id change; server version bump on updates (client cannot forge)

create or replace function thecount_set_updated_at()
returns trigger as $$
begin
  new.updated_at = now();
  return new;
end;
$$ language plpgsql;

create or replace function thecount_bump_server_version()
returns trigger as $$
begin
  if tg_op = 'UPDATE' then
    -- prevent user_id change
    if new.user_id is distinct from old.user_id then
      raise exception 'cannot change user_id';
    end if;
    new.server_version = coalesce(old.server_version, 0) + 1;
  elsif tg_op = 'INSERT' then
    new.server_version = coalesce(new.server_version, 0);
  end if;
  return new;
end;
$$ language plpgsql;

create trigger trg_profiles_updated_at before update on profiles for each row execute function thecount_set_updated_at();
create trigger trg_daily_strokes_updated_at before update on daily_strokes for each row execute function thecount_set_updated_at();
create trigger trg_payments_updated_at before update on payments for each row execute function thecount_set_updated_at();

create trigger trg_daily_strokes_version before insert or update on daily_strokes for each row execute function thecount_bump_server_version();
create trigger trg_payments_version before insert or update on payments for each row execute function thecount_bump_server_version();
