-- ============================================================
-- V21__User_Last_Login_Ip.sql
-- Captures the IP address of the most recent successful login.
-- ============================================================

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_login_ip VARCHAR(45);
