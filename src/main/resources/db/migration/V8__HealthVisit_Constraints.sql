-- ============================================================
-- V8__HealthVisit_Constraints.sql
-- 1. Add CHECK constraint for visit_type enum values
-- 2. Add optimistic locking version column to health_visits
-- ============================================================

ALTER TABLE health_visits
    ADD CONSTRAINT chk_hv_visit_type
    CHECK (visit_type IN ('ANC', 'PNC', 'IMMUNIZATION', 'SICK_CHILD', 'GROWTH_MONITORING'));

ALTER TABLE health_visits
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
