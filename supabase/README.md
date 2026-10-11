# Supabase: Migrations (Task 5A)

Run these in the Supabase SQL Editor in order: 001_schema.sql, 002_triggers.sql, 003_rls.sql.

Confirm RLS is ON for each table (in Table Editor / by querying `pg_tables` with `rowsecurity` or use Supabase UI). 

Notes
- These files create schema, triggers (server version bump, prevent user_id change, updated_at), and RLS policies. Ordinary users cannot read/write invitations, admins, or app_config.
- Soft deletes use deleted_at; clients update deleted_at rather than hard-delete.
- sync columns present as specified.
