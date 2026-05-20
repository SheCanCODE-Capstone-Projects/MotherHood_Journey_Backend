-- ============================================================
-- V7__Performance_Indexes.sql
-- Add missing indexes on health_visits for visit_type queries.
-- Covers: filter by visit_type alone, and compound facility+type.
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_visit_type
    ON health_visits (visit_type);

CREATE INDEX IF NOT EXISTS idx_visit_facility_type
    ON health_visits (facility_id, visit_type);
