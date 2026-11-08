-- =====================================================
--   DatabaseSetup.sql — Personal Expense Analyzer
--   Run this FIRST in MySQL Workbench!
-- =====================================================

CREATE DATABASE IF NOT EXISTS expense_db;
USE expense_db;

CREATE TABLE IF NOT EXISTS expenses (
    id           INT            AUTO_INCREMENT PRIMARY KEY,
    amount       DECIMAL(10,2)  NOT NULL,
    category     VARCHAR(50)    NOT NULL,
    description  VARCHAR(200),
    expense_date DATE           NOT NULL
);

-- Sample data so charts show immediately!
INSERT INTO expenses (amount, category, description, expense_date) VALUES
    (450.00,  'Food',          'Lunch at restaurant',    CURDATE()),
    (120.00,  'Transport',     'Uber ride',              CURDATE()),
    (1500.00, 'Shopping',      'New shoes',              CURDATE()),
    (300.00,  'Food',          'Groceries',              CURDATE()),
    (800.00,  'Bills',         'Electricity bill',       DATE_SUB(CURDATE(), INTERVAL 1 DAY)),
    (200.00,  'Entertainment', 'Movie tickets',          DATE_SUB(CURDATE(), INTERVAL 2 DAY)),
    (600.00,  'Health',        'Medical checkup',        DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
    (350.00,  'Food',          'Dinner with friends',    DATE_SUB(CURDATE(), INTERVAL 3 DAY)),
    (1200.00, 'Education',     'Online course',          DATE_SUB(CURDATE(), INTERVAL 5 DAY)),
    (90.00,   'Transport',     'Auto rickshaw',          DATE_SUB(CURDATE(), INTERVAL 6 DAY)),
    (500.00,  'Shopping',      'Books',                  DATE_SUB(CURDATE(), INTERVAL 7 DAY)),
    (250.00,  'Other',         'Miscellaneous',          DATE_SUB(CURDATE(), INTERVAL 8 DAY));

SELECT * FROM expenses;

-- =====================================================
-- The app uses these SQL queries:
--
-- INSERT → Add new expense
-- SELECT → Load all expenses into table
-- DELETE → Remove an expense
-- GROUP BY SUM → Calculate totals per category (CHARTS!)
-- MONTH()  → Filter by month
-- =====================================================
