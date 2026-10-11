-- 005_registration_guard.sql
-- Server-side registration guard. Prefer Auth Hook Before User Created; the trigger here is a safety net.
-- Profile creation must happen server-side. Since Before User Created runs before auth.users exists,
-- we rely on the hook to reject uninvited/over-limit signups and use an AFTER INSERT trigger on auth.users
-- to create profiles only for allowed cases (or let the hook coordinate via a reservation table).

-- Reservation table to coordinate with Before User Created hook atomically
create table if not exists pending_registrations (
  id uuid primary key default gen_random_uuid(),
  email text not null check (email = lower(email)),
  invited_at timestamptz not null default now(),
  expires_at timestamptz not null default now() + interval '10 minutes',
  consumed_at timestamptz,
  consumed_by uuid references auth.users(id)
);

create unique index if not exists idx_pending_registrations_email on pending_registrations(email) where consumed_at is null;

-- After insert on auth.users: create profile if there is a valid pending/authorized registration
-- Note: actual enforcement should be in Before User Created hook with transaction + lock. This is fallback.
create or replace function thecount_create_profile_on_signup()
returns trigger as $$
begin
  -- Only create if not already exists
  if not exists (select 1 from profiles where id = new.id) then
    -- try to find a consumed pending registration for this email
    update pending_registrations pr
       set consumed_by = new.id, consumed_at = now()
     where lower(pr.email) = lower(new.email)
       and pr.consumed_at is null
       and pr.expires_at > now();
    if found then
      insert into profiles (id, email) values (new.id, lower(new.email));
    end if;
  end if;
  return new;
end;
$$ language plpgsql security definer set search_path = public;

drop trigger if exists trg_auth_users_create_profile on auth.users;
create trigger trg_auth_users_create_profile after insert on auth.users for each row execute function thecount_create_profile_on_signup();

-- Helper functions for admin operations (dashboard use)
-- (admin management is dashboard-only; RLS prevents client access)
