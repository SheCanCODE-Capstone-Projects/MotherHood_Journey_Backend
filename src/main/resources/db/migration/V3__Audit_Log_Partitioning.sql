-- ============================================================
-- V3__Audit_Log_Partitioning.sql
-- Convert audit_log to a range-partitioned table by created_at month.
-- Creates partitions for the current month and next 3 months.
-- A scheduled job (or DBA script) should create future partitions monthly.
-- ============================================================

-- Step 1: Rename existing table
ALTER TABLE audit_log RENAME TO audit_log_old;

-- Step 2: Create partitioned parent table
CREATE TABLE audit_log (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL REFERENCES users(id),
    action          VARCHAR(32)  NOT NULL,
    resource_type   VARCHAR(32)  NOT NULL,
    resource_id     UUID,
    geo_location_id UUID         REFERENCES geo_locations(id),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(255),
    success         BOOLEAN      NOT NULL DEFAULT TRUE,
    fail_reason     VARCHAR(128),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (created_at);

-- Step 3: Indexes on parent (inherited by partitions)
CREATE INDEX IF NOT EXISTS idx_audit_user        ON audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_resource    ON audit_log (resource_type);
CREATE INDEX IF NOT EXISTS idx_audit_resource_id ON audit_log (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_ts          ON audit_log (created_at);

-- Step 4: Create monthly partitions (current + next 3 months)
CREATE TABLE audit_log_2026_05 PARTITION OF audit_log
    FOR VALUES FROM ('2026-05-01') TO ('2026-06-01');

CREATE TABLE audit_log_2026_06 PARTITION OF audit_log
    FOR VALUES FROM ('2026-06-01') TO ('2026-07-01');

CREATE TABLE audit_log_2026_07 PARTITION OF audit_log
    FOR VALUES FROM ('2026-07-01') TO ('2026-08-01');

CREATE TABLE audit_log_2026_08 PARTITION OF audit_log
    FOR VALUES FROM ('2026-08-01') TO ('2026-09-01');

-- Step 5: Migrate existing data
INSERT INTO audit_log SELECT * FROM audit_log_old;

-- Step 6: Drop old table
DROP TABLE audit_log_old;
