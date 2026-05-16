-- ============================================================
-- V2__Schema_Fixes.sql
-- Fix: children.health_id missing column
-- Fix: service_request_docs.file_path length 32 → 255
-- Fix: diagnoses.description length 32 → 255
-- ============================================================

ALTER TABLE children
    ADD COLUMN IF NOT EXISTS health_id VARCHAR(32) UNIQUE;

CREATE UNIQUE INDEX IF NOT EXISTS idx_child_health_id ON children (health_id);

ALTER TABLE service_request_docs
    ALTER COLUMN file_path TYPE VARCHAR(255);

ALTER TABLE diagnoses
    ALTER COLUMN description TYPE VARCHAR(255);
