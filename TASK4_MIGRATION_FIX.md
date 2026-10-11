Task 4 migration fix - schema-based migration

Fixes
- Added MIGRATION_1_2 in data/db/Migrations.kt creating payments table ONLY (no touches to daily_strokes). SQL matches Room schema for v2: id is BLOB (UUID stored as BLOB by Room), all columns match entity types/names. No index for payments in generated schema, so no CREATE INDEX added. No fallbackToDestructiveMigration used - confirmed none in project.

Verification
- Searched entire project: no fallbackToDestructiveMigration found (rg-style search, none returned).
- Enabled exportSchema = true with schema location; generated schema inspected at app/schemas/com.sandesh.thecount.data.db.TheCountDatabase/2.json. The payments table CREATE TABLE matches exactly what was added to migration (id BLOB PK, user_id/payment_date/amount as TEXT/appropriate types, timestamps/flags as INTEGER/TEXT).
- Registered migration via .addMigration(MIGRATION_1_2) in TheCountDatabase.

Build & tests (real results)
- assembleDebug: BUILD SUCCESSFUL
- testDebugUnitTest: BUILD SUCCESSFUL - ValidatorsTest 9/9, EarningsCalculatorTest 9/9, ExampleUnitTest 1/1 (19 tests total, 0 failures)

Files
- app/src/main/java/com/sandesh/thecount/data/db/Migrations.kt (new)
- app/src/main/java/com/sandesh/thecount/data/db/TheCountDatabase.kt (modified to add migration, exportSchema true)
- app/build.gradle.kts (modified to set schema location)
- docs/STATE.md (Task 4 set to built, awaiting approval)

MIGRATION_1_2 SQL
```sql
CREATE TABLE IF NOT EXISTS payments (
  id BLOB NOT NULL PRIMARY KEY,
  user_id TEXT NOT NULL,
  payment_date TEXT NOT NULL,
  amount TEXT NOT NULL,
  note TEXT,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL,
  version INTEGER NOT NULL,
  sync_state TEXT NOT NULL,
  server_version INTEGER,
  deleted_at INTEGER
)
```

STOP - migration added. Task 4 not marked done; awaiting approval.
