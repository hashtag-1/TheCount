
# TheCount Backup and Recovery

## 1. Cloud sync is not a backup

Cloud synchronization keeps local records and Supabase records aligned. A deletion, incorrect edit, or synchronization mistake can also reach the cloud.

A JSON backup is a separate copy that the user can save outside the application. Keep copies on another device or in secure cloud storage.

Before exporting, use Sync now and verify that pending records have synchronized. Export contains active local records, so records that have not downloaded to this device will not be included.

## 2. Export

1. Open Settings.
2. Select Export backup.
3. Choose a filename and location.
4. Store the exported JSON file somewhere safe.

The backup contains the application name, format version, export timestamp, source account ID, strokes, payments, and SHA-256 checksum. Payment amounts are decimal strings with two decimal places.

## 3. Import

1. Open Settings and select Import backup.
2. Choose the JSON file.
3. Review the record counts and date range.
4. Select MERGE or REPLACE.
5. Confirm account ownership if the backup came from another account.
6. For REPLACE, type REPLACE to confirm.
7. Wait for import to finish and synchronization to run.

MERGE adds missing strokes by date and payments by ID. Existing active records are preserved.

REPLACE creates an automatic local safety copy, marks records missing from the backup for deletion, and restores the backup's records. Use this mode carefully.

Imported and changed records are marked pending so the normal sync worker can upload them.

## 4. Validation and security

Imports must have the supported application name and format version, a valid checksum, valid dates on or after 2026-09-02, non-negative stroke counts, positive payment amounts with two decimal places, and no duplicate stroke dates.

Imported rows are assigned to the currently signed-in account. A different source account requires explicit confirmation.

The checksum detects accidental changes; it is not an authentication signature. Do not import untrusted backup files.

## 5. Supabase plan limitations

Supabase Free projects do not include automatic daily database backups and may be paused after approximately seven days of insufficient activity. Pro projects include daily backups retained for seven days; Team projects for 14 days; Enterprise projects for up to 30 days.

References:
- https://supabase.com/docs/guides/platform/backups
- https://supabase.com/docs/guides/platform/free-project-pausing
- https://supabase.com/pricing

These are platform-level backups, not a substitute for a user-managed export. Maintain independent JSON backups, particularly when using the Free plan.

## 6. Important limitation

The app's JSON backup contains strokes and payments, not the complete Supabase project, authentication users, database schema, RLS policies, or other backend configuration. The automatic REPLACE safety copy is stored in the app's cache and may be removed by Android. Export an independent backup before performing risky operations.
