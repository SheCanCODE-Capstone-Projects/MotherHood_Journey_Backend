-- ============================================================
-- V17__Soft_Delete_Mother_Pregnancy.sql
-- Adds deleted_at columns for soft delete on Mother and Pregnancy.
-- ============================================================

ALTER TABLE mothers
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

ALTER TABLE pregnancies
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_mothers_deleted_at ON mothers (deleted_at);
CREATE INDEX IF NOT EXISTS idx_pregnancies_deleted_at ON pregnancies (deleted_at);
