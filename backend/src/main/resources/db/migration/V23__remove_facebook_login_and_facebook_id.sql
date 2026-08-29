-- V23: Remove facebook_id column and its unique constraint from users table
ALTER TABLE users DROP CONSTRAINT IF EXISTS ukjmubronqnn4q0cwe2egqsgvnl;
ALTER TABLE users DROP COLUMN IF EXISTS facebook_id;
