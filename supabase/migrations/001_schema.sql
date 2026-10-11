-- 001_schema.sql
-- TheCount: tables, constraints, indexes. Run ONCE in the Supabase SQL editor.
-- Server-side columns only. Client-only sync fields (syncState etc.) live in Room, not here.

-- One global counter. Every insert/update on a synced table takes the next number (see 002).
-- Clients pull "everything with version > my last cursor", and a conflict is "server version
-- differs from the version I based my edit on" (docs/DECISIONS.md D9).
create sequence if not exists public.thecount_version_seq;

create table if not exists public.profiles (
  id           uuid primary key references auth.users(id) on delete cascade,
  email        text not null unique check (email = lower(email)),
  display_name text,
  status       text not null default 'active' check (status in ('active', 'suspended')),
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now()
);

create table if not exists public.daily_strokes (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users(id) on delete cascade,
  work_date    date not null,
  stroke_count bigint not null check (stroke_count >= 0),
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  deleted_at   timestamptz,
  version      bigint not null default 0,   -- assigned by trigger, never by the client
  constraint daily_strokes_user_work_date_uk unique (user_id, work_date),
  constraint daily_strokes_work_date_after_start check (work_date >= date '2026-09-02')
);

create table if not exists public.payments (
  id           uuid primary key default gen_random_uuid(),
  user_id      uuid not null references auth.users(id) on delete cascade,
  payment_date date not null,
  amount       numeric(14,2) not null check (amount > 0),
  note         text check (note is null or char_length(note) <= 500),
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  deleted_at   timestamptz,
  version      bigint not null default 0,   -- assigned by trigger, never by the client
  constraint payments_payment_date_after_start check (payment_date >= date '2026-09-02')
);

-- Shells for Task 5B (no client access, see 003).
create table if not exists public.invitations (
  id         uuid primary key default gen_random_uuid(),
  email      text not null check (email = lower(email)),
  status     text not null default 'active' check (status in ('active', 'used', 'revoked')),
  created_at timestamptz not null default now(),
  expires_at timestamptz,
  used_at    timestamptz,
  used_by    uuid references auth.users(id) on delete set null
);

create table if not exists public.admins (
  id         uuid primary key references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists public.app_config (
  key   text primary key,
  value text not null
);

-- Indexes (the unique constraints above already index (user_id, work_date)).
create index if not exists idx_daily_strokes_user_version on public.daily_strokes (user_id, version);
create index if not exists idx_payments_user_payment_date on public.payments (user_id, payment_date);
create index if not exists idx_payments_user_version      on public.payments (user_id, version);
create unique index if not exists idx_invitations_one_active_per_email
  on public.invitations (email) where status = 'active';
