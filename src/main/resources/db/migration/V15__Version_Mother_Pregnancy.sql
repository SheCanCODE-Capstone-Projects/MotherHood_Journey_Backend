-- ============================================================
-- V15__Version_Mother_Pregnancy.sql
-- Add @Version columns for optimistic locking on Mother and Pregnancy.
-- ============================================================

ALTER TABLE mothers
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE pregnancies
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
