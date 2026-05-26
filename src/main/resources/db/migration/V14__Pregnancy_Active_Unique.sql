-- ============================================================
-- V14__Pregnancy_Active_Unique.sql
-- Enforce: a mother may have at most one ACTIVE pregnancy at a time.
-- ============================================================

CREATE UNIQUE INDEX IF NOT EXISTS uq_pregnancies_active_per_mother
    ON pregnancies (mother_id)
    WHERE status = 'ACTIVE';
