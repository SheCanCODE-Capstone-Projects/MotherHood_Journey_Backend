-- ============================================================
-- V5__Appointments_And_Notifications.sql
-- 1. Create appointments table
-- 2. Create sms_notifications table
-- ============================================================

-- ── 1. appointments ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS appointments (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    patient_ref_id   UUID         NOT NULL,
    patient_type     VARCHAR(8)   NOT NULL CHECK (patient_type IN ('MOTHER', 'CHILD')),
    facility_id      UUID         NOT NULL REFERENCES facilities(id),
    health_worker_id UUID         REFERENCES users(id),
    geo_location_id  UUID         REFERENCES geo_locations(id),
    scheduled_at     TIMESTAMP    NOT NULL,
    appointment_type VARCHAR(32)  NOT NULL,
    status           VARCHAR(16)  NOT NULL DEFAULT 'SCHEDULED',
    reminder_sent    BOOLEAN      NOT NULL DEFAULT FALSE,
    notes            TEXT,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_appt_patient    ON appointments (patient_ref_id);
CREATE INDEX IF NOT EXISTS idx_appt_facility   ON appointments (facility_id);
CREATE INDEX IF NOT EXISTS idx_appt_datetime   ON appointments (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appt_status     ON appointments (status);

-- ── 2. sms_notifications ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS sms_notifications (
    id                  UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    recipient_user_id   UUID         NOT NULL REFERENCES users(id),
    phone_number        VARCHAR(20)  NOT NULL,
    message_body        TEXT         NOT NULL,
    notification_type   VARCHAR(32)  NOT NULL,
    status              VARCHAR(16)  NOT NULL DEFAULT 'QUEUED',
    at_message_id       VARCHAR(64),
    scheduled_at        TIMESTAMP    NOT NULL,
    sent_at             TIMESTAMP,
    retry_count         INTEGER      NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sms_user      ON sms_notifications (recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_sms_status    ON sms_notifications (status);
CREATE INDEX IF NOT EXISTS idx_sms_scheduled ON sms_notifications (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_sms_type      ON sms_notifications (notification_type);
