-- ============================================
-- Script: Reset Database
-- Description: Drop and recreate the database
-- Usage: Run this script to completely reset the database
-- ============================================
-- Drop the database if it exists
DROP DATABASE IF EXISTS se330db;

-- Create fresh database
CREATE DATABASE se330db CHARACTER
SET
    utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Select the new database
USE se330db;

-- Confirmation message
SELECT
    'Database se330db has been reset successfully!' AS Status;