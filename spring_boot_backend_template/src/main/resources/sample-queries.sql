-- Optional reference SQL queries for manual validation against MySQL database `finance_backend`.

-- Total income
SELECT COALESCE(SUM(amount), 0) AS total_income
FROM financial_records
WHERE deleted = 0 AND type = 'INCOME';

-- Total expenses
SELECT COALESCE(SUM(amount), 0) AS total_expenses
FROM financial_records
WHERE deleted = 0 AND type = 'EXPENSE';

-- Net balance
SELECT
    COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) -
    COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS net_balance
FROM financial_records
WHERE deleted = 0;

-- Category wise totals
SELECT category, COALESCE(SUM(amount), 0) AS total
FROM financial_records
WHERE deleted = 0
GROUP BY category
ORDER BY total DESC;

-- Monthly trends
SELECT
    YEAR(record_date) AS year_value,
    MONTH(record_date) AS month_value,
    COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END), 0) AS income_total,
    COALESCE(SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS expense_total
FROM financial_records
WHERE deleted = 0
GROUP BY YEAR(record_date), MONTH(record_date)
ORDER BY year_value, month_value;
