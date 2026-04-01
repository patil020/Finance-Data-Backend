-- Additional sample data script for finance_backend (MySQL).
-- Note: This script is intended for manual execution after application startup.
-- The app already creates default users via DataInitializer.

-- Extra INCOME records
INSERT INTO financial_records (amount, type, category, record_date, note, created_by, deleted, created_at, updated_at)
SELECT 18000.00, 'INCOME', 'Dividend', '2026-02-10', 'Quarterly dividend credit', u.id, 0, NOW(), NOW()
FROM users u
WHERE u.email = 'admin@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM financial_records fr
      WHERE fr.note = 'Quarterly dividend credit'
  );

INSERT INTO financial_records (amount, type, category, record_date, note, created_by, deleted, created_at, updated_at)
SELECT 25000.00, 'INCOME', 'Bonus', '2026-03-05', 'Performance bonus', u.id, 0, NOW(), NOW()
FROM users u
WHERE u.email = 'analyst@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM financial_records fr
      WHERE fr.note = 'Performance bonus'
  );

-- Extra EXPENSE records
INSERT INTO financial_records (amount, type, category, record_date, note, created_by, deleted, created_at, updated_at)
SELECT 3200.00, 'EXPENSE', 'Transport', '2026-03-09', 'Cab and fuel expenses', u.id, 0, NOW(), NOW()
FROM users u
WHERE u.email = 'viewer@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM financial_records fr
      WHERE fr.note = 'Cab and fuel expenses'
  );

INSERT INTO financial_records (amount, type, category, record_date, note, created_by, deleted, created_at, updated_at)
SELECT 7800.00, 'EXPENSE', 'Medical', '2026-03-14', 'Medical and pharmacy bills', u.id, 0, NOW(), NOW()
FROM users u
WHERE u.email = 'admin@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM financial_records fr
      WHERE fr.note = 'Medical and pharmacy bills'
  );

INSERT INTO financial_records (amount, type, category, record_date, note, created_by, deleted, created_at, updated_at)
SELECT 5400.00, 'EXPENSE', 'Entertainment', '2026-03-20', 'Weekend outings and movies', u.id, 0, NOW(), NOW()
FROM users u
WHERE u.email = 'analyst@example.com'
  AND NOT EXISTS (
      SELECT 1
      FROM financial_records fr
      WHERE fr.note = 'Weekend outings and movies'
  );
