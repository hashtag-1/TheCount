Task 5A: Supabase schema and RLS (SQL only)

Files created
- supabase/migrations/001_schema.sql - tables: profiles, daily_strokes, payments, invitations, admins, app_config with constraints (unique (user_id,work_date), stroke_count>=0, work_date>=2026-09-02, amount>0, payment_date>=2026-09-02, email lowercase, status constrained). Foreign keys to auth.users with ON DELETE CASCADE. Indexes: (user_id,work_date), (user_id,version) for strokes/payments, active-email partial unique index on invitations.
- supabase/migrations/002_triggers.sql - updated_at trigger on all relevant tables; bump server_version on INSERT/UPDATE for strokes/payments; prevent user_id change on UPDATE.
- supabase/migrations/003_rls.sql - RLS enabled on all tables. Helper function thecount_has_active_profile (SECURITY DEFINER, search_path=public). Policies: users read/modify only their own rows where active profile exists (profiles, daily_strokes, payments). No policies for invitations/admins/app_config (RLS on but not exposed to ordinary users).
- supabase/README.md - run order and confirmation steps.

Notes
- Cannot run SQL against Supabase (no credentials). User must run in SQL Editor and confirm RLS on each table.
- Soft delete via deleted_at; no hard-delete path for ordinary users. Clients must UPDATE deleted_at.

All changes are SQL only (no Android code). Built/tested Android unchanged (unit tests still pass as before).

STOP - Task 5A done (SQL written). Await user running migrations and confirmation.
