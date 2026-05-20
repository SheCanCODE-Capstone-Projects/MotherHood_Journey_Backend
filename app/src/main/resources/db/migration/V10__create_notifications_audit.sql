
--  SMS_NOTIFICATIONS TABLE
CREATE TABLE IF NOT EXISTS sms_notifications (
        id  UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
        recipient_user_id UUID        NOT NULL,
        phone_number      VARCHAR(20) NOT NULL,
        message_body      TEXT        NOT NULL,
        notification_type VARCHAR(32) NOT NULL,
        status            VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
        at_message_id     VARCHAR(64),
        scheduled_at      TIMESTAMP   NOT NULL,
        sent_at           TIMESTAMP,
        retry_count       INT         NOT NULL DEFAULT 0,
        created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Constraints
  CONSTRAINT chk_sms_notification_type CHECK (
             notification_type IN (
                    'VACCINATION_REMINDER',
                    'APPOINTMENT',
                    'HEALTH_TIP',
                    'SERVICE_STATUS',
                    'EMERGENCY'
             )
      ),

  CONSTRAINT chk_sms_status CHECK (
            status IN ('QUEUED','SENT','DELIVERED','FAILED')
      ),

  CONSTRAINT chk_sms_retry CHECK (retry_count >= 0),

    -- sent_at must be after or equal to scheduled_at
  CONSTRAINT chk_sms_sent_after_scheduled CHECK (
         sent_at IS NULL OR sent_at >= scheduled_at
         ),

  CONSTRAINT fk_sms_recipient
        FOREIGN KEY (recipient_user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_sms_user
    ON sms_notifications (recipient_user_id);

CREATE INDEX IF NOT EXISTS idx_sms_status
    ON sms_notifications (status);

CREATE INDEX IF NOT EXISTS idx_sms_scheduled
    ON sms_notifications (scheduled_at);

CREATE INDEX IF NOT EXISTS idx_sms_type
    ON sms_notifications (notification_type);

-- find QUEUED messages ready to send
CREATE INDEX IF NOT EXISTS idx_sms_queued_ready
    ON sms_notifications (scheduled_at)
    WHERE status = 'QUEUED';

--  AUDIT_LOG TABLE  (append-only, PHI access log)

CREATE TABLE IF NOT EXISTS audit_log (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    action          VARCHAR(32) NOT NULL,
    resource_type   VARCHAR(64) NOT NULL,
    resource_id     UUID,
    geo_location_id UUID,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(512),
    success         BOOLEAN     NOT NULL DEFAULT TRUE,
    fail_reason     VARCHAR(256),
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_audit_action CHECK (
        action IN (
            'READ',
            'CREATE',
            'UPDATE',
            'DELETE',
            'EXPORT',
            'LOGIN',
            'LOGOUT'
         )
    ),

    -- Valid IPv4 or IPv6
     CONSTRAINT chk_audit_ip CHECK (
        ip_address IS NULL OR
         ip_address ~ '^(\d{1,3}\.){3}\d{1,3}$'
         OR ip_address ~ '^[0-9a-fA-F:]+$'
     ),

    CONSTRAINT fk_audit_user
         FOREIGN KEY (user_id)
         REFERENCES users (id)
         ON DELETE RESTRICT
         ON UPDATE CASCADE,

    CONSTRAINT fk_audit_geo_location
    FOREIGN KEY (geo_location_id)
    REFERENCES geo_locations (id)
    ON DELETE SET NULL
     ON UPDATE CASCADE
);

--  Indexes
CREATE INDEX IF NOT EXISTS idx_audit_user
    ON audit_log (user_id);

CREATE INDEX IF NOT EXISTS idx_audit_resource
    ON audit_log (resource_type);

CREATE INDEX IF NOT EXISTS idx_audit_resource_id
    ON audit_log (resource_type, resource_id);

CREATE INDEX IF NOT EXISTS idx_audit_ts
    ON audit_log (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_action
    ON audit_log (action);

CREATE RULE audit_log_no_update AS
    ON UPDATE TO audit_log DO INSTEAD NOTHING;

CREATE RULE audit_log_no_delete AS
    ON DELETE TO audit_log DO INSTEAD NOTHING;

--  Comments
COMMENT ON TABLE sms_notifications IS
    'Outbound SMS via Africa''s Talking API. Cron job scans '
        'QUEUED rows where scheduled_at <= NOW() and calls AT API. '
        'at_message_id enables webhook-based delivery status updates.';

COMMENT ON TABLE audit_log IS
    'PHI access audit log. Required for Rwanda Data Protection '
        'Law compliance. Immutable — no UPDATE or DELETE permitted. '
        'Retention: 7 years per MoH policy. '
        'Partition by month in production.';

COMMENT ON COLUMN audit_log.resource_type IS
    'Table name of the accessed resource, '
        'e.g. mothers, children, gov_reports, vaccination_records.';