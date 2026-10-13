-- Run with a MySQL administrative account. Replace the sample password FIRST.
CREATE DATABASE IF NOT EXISTS patentpulse CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'patentpulse'@'localhost' IDENTIFIED BY 'REPLACE_WITH_A_STRONG_DATABASE_PASSWORD';
-- Application users cannot drop tables, alter schema, or delete records.
GRANT SELECT, INSERT, UPDATE ON patentpulse.* TO 'patentpulse'@'localhost';
-- Apply schema.sql separately as a database administrator.
