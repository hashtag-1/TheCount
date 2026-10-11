-- 001_schema.sql
-- TheCount: schema, constraints, indexes (SQL only)

create table if not exists profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text not null unique check (email = lower(email)),
  display_name text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  version integer not null default 0,
  sync_state text not null default 'SYNCED',
  server_version integer,
  deleted_at timestamptz
);

create table if not exists daily_strokes (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  work_date date not null,
  stroke_count bigint not null check (stroke_count >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  version integer not null default 0,
  sync_state text not null default 'SYNCED',
  server_version integer,
  deleted_at timestamptz,
  constraint daily_strokes_user_work_date_uk unique (user_id, work_date),
  constraint daily_strokes_work_date_after_start check (work_date >= '2026-09-02')
);

create table if not exists payments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users(id) on delete cascade,
  payment_date date not null,
  amount numeric(14,2) not null check (amount > 0),
  note text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  version integer not null default 0,
  sync_state text not null default 'SYNCED',
  server_version integer,
  deleted_at timestamptz,
  constraint payments_payment_date_after_start check (payment_date >= '2026-09-02')
);

create table if not exists invitations (
  id uuid primary key default gen_random_uuid(),
  email text not null check (email = lower(email)),
  status text not null default 'active' check (status in ('active','used','revoked')),
  created_at timestamptz not null default now(),
  expires_at timestamptz,
  used_at timestamptz,
  used_by uuid references auth.users(id)
);

create table if not exists admins (
  id uuid primary key references auth.users(id) on delete cascade,
  created_at timestamptz not null default now()
);

create table if not exists app_config (
  key text primary key,
  value text not null
);

create index if not exists idx_daily_strokes_user_work_date on daily_strokes(user_id, work_date);
create index if not exists idx_daily_strokes_user_version on daily_strokes(user_id, version);
create index if not exists idx_payments_user_payment_date on payments(user_id, payment_date);
create index if not exists idx_payments_user_version on payments(user_id, version);
create unique index if not exists idx_invitations_active_email on invitations(email) where status = 'active';
