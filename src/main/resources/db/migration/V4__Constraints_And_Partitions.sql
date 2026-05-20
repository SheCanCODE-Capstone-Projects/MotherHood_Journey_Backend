-- ============================================================
-- V4__Constraints_And_Partitions.sql
-- 1. Add CHECK constraints for polymorphic patient_type columns
-- 2. Extend audit_log monthly partitions through December 2027
-- ============================================================

-- ── 1. patient_type CHECK constraints ────────────────────────
ALTER TABLE health_visits
    ADD CONSTRAINT chk_hv_patient_type
    CHECK (patient_type IN ('MOTHER', 'CHILD'));

ALTER TABLE appointments
    ADD CONSTRAINT chk_appt_patient_type
    CHECK (patient_type IN ('MOTHER', 'CHILD'));

-- ── 2. audit_log partitions: 2026-09 through 2027-12 ─────────
CREATE TABLE IF NOT EXISTS audit_log_2026_09 PARTITION OF audit_log
    FOR VALUES FROM ('2026-09-01') TO ('2026-10-01');

CREATE TABLE IF NOT EXISTS audit_log_2026_10 PARTITION OF audit_log
    FOR VALUES FROM ('2026-10-01') TO ('2026-11-01');

CREATE TABLE IF NOT EXISTS audit_log_2026_11 PARTITION OF audit_log
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');

CREATE TABLE IF NOT EXISTS audit_log_2026_12 PARTITION OF audit_log
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_01 PARTITION OF audit_log
    FOR VALUES FROM ('2027-01-01') TO ('2027-02-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_02 PARTITION OF audit_log
    FOR VALUES FROM ('2027-02-01') TO ('2027-03-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_03 PARTITION OF audit_log
    FOR VALUES FROM ('2027-03-01') TO ('2027-04-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_04 PARTITION OF audit_log
    FOR VALUES FROM ('2027-04-01') TO ('2027-05-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_05 PARTITION OF audit_log
    FOR VALUES FROM ('2027-05-01') TO ('2027-06-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_06 PARTITION OF audit_log
    FOR VALUES FROM ('2027-06-01') TO ('2027-07-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_07 PARTITION OF audit_log
    FOR VALUES FROM ('2027-07-01') TO ('2027-08-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_08 PARTITION OF audit_log
    FOR VALUES FROM ('2027-08-01') TO ('2027-09-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_09 PARTITION OF audit_log
    FOR VALUES FROM ('2027-09-01') TO ('2027-10-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_10 PARTITION OF audit_log
    FOR VALUES FROM ('2027-10-01') TO ('2027-11-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_11 PARTITION OF audit_log
    FOR VALUES FROM ('2027-11-01') TO ('2027-12-01');

CREATE TABLE IF NOT EXISTS audit_log_2027_12 PARTITION OF audit_log
    FOR VALUES FROM ('2027-12-01') TO ('2028-01-01');

-- ── 3. Add version column to service_requests for optimistic locking ──
ALTER TABLE service_requests
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ── 4. Add service_request_id FK to gov_sync_log ─────────────
ALTER TABLE gov_sync_log
    ADD COLUMN IF NOT EXISTS service_request_id UUID REFERENCES service_requests(id);

CREATE INDEX IF NOT EXISTS idx_gsync_sr ON gov_sync_log (service_request_id);
