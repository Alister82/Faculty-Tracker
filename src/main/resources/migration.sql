-- Fix the 'role' column to VARCHAR in case Hibernate created it as ENUM
-- This runs at startup and is safe to run multiple times (Hibernate update mode)
ALTER TABLE app_user MODIFY COLUMN role VARCHAR(20) NOT NULL;
