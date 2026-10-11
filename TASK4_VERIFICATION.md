Task 4 verification (confirm it's closed properly)

Migration correctness
- MIGRATION_1_2 in app/src/main/java/com/sandesh/thecount/data/db/Migrations.kt creates payments table with SQL matching Room schema v2: id is BLOB (PK), user_id/payment_date/amount TEXT NOT NULL, note TEXT nullable, timestamps/flags as INTEGER/TEXT with correct nullability. No destructive migration, no fallbackToDestructiveMigration present. Schema at app/schemas/com.sandesh.thecount.data.db.TheCountDatabase/2.json confirms structure.
- Migration is registered via .addMigration(MIGRATION_1_2) in TheCountDatabase (v2, exportSchema true).
- No other DB changes; daily_strokes untouched by migration.

Implementation (Task 4 parts)
- PaymentEntity/Dao/Repository added. BigDecimalConverter added. PaymentValidators (amount > 0, max 2 decimals, date >= 2026-09-02 and not future). Earnings now include real payments. Home/Payments/AddPayment screens wired to ViewModel. ViewModelFactory updated.

Testing
- Clean build + tests: BUILD SUCCESSFUL. Unit tests: EarningsCalculatorTest 9/9, ValidatorsTest 9/9, ExampleUnitTest 1/1 (19/19 passed). No test failures.

Git history
- 760889f "Fix Task 4: add Room migration 1->2 for payments (no destructive)" exists (migration/schema added). feee475 marked Task 4 done (approved). 93900b9 implemented Task 4. Current HEAD is 9ba860f (Task 5A).

Conclusion
- Task 4 is properly closed: migration correct and non-destructive, code complete, all unit tests pass. STATE.md shows Task 4 done (approved). Task 5A SQL written; awaiting user to run SQL.

No code changes made.
