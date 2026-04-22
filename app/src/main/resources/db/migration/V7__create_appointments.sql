
CREATE TABLE IF NOT EXISTS appointments (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_ref_id   UUID        NOT NULL,
    patient_type     VARCHAR(8)  NOT NULL,
    facility_id      UUID        NOT NULL,
    health_worker_id UUID,
    geo_location_id  UUID,
    scheduled_at     TIMESTAMP   NOT NULL,
    appointment_type VARCHAR(32) NOT NULL,
    status           VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
    reminder_sent    BOOLEAN     NOT NULL DEFAULT FALSE,
    notes            TEXT,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),

    --  Constraints
    CONSTRAINT chk_appt_patient_type CHECK (
        patient_type IN ('MOTHER','CHILD')
    ),

    CONSTRAINT chk_appt_type CHECK (
       appointment_type IN (
       'ANC',
       'PNC',
       'VACCINATION',
       'GROWTH_CHECK',
       'FOLLOW_UP'
         )
    ),

    CONSTRAINT chk_appt_status CHECK (
       status IN ('SCHEDULED','COMPLETED','NO_SHOW','CANCELLED')
    ),

    CONSTRAINT fk_appt_facility
    FOREIGN KEY (facility_id)
    REFERENCES facilities (id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT fk_appt_health_worker
    FOREIGN KEY (health_worker_id)
    REFERENCES users (id)
    ON DELETE SET NULL
    ON UPDATE CASCADE,

    CONSTRAINT fk_appt_geo_location
    FOREIGN KEY (geo_location_id)
    REFERENCES geo_locations (id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
    );

--  Indexes
CREATE INDEX IF NOT EXISTS idx_appt_patient
    ON appointments (patient_ref_id);

CREATE INDEX IF NOT EXISTS idx_appt_facility
    ON appointments (facility_id);

CREATE INDEX IF NOT EXISTS idx_appt_datetime
    ON appointments (scheduled_at);

CREATE INDEX IF NOT EXISTS idx_appt_status
    ON appointments (status);

CREATE INDEX IF NOT EXISTS idx_appt_type
    ON appointments (appointment_type);

-- find SCHEDULED appointments where reminder has not been sent yet

CREATE INDEX IF NOT EXISTS idx_appt_reminder_pending
    ON appointments (scheduled_at)
    WHERE status = 'SCHEDULED' AND reminder_sent = FALSE;

-- Comments
COMMENT ON TABLE appointments IS
    'Appointment scheduler drives the analytics dashboard no-show '
    'rate and capacity planning. reminder_sent flag is set by the '
    'notification cron job 24 h before scheduled_at.';

COMMENT ON COLUMN appointments.reminder_sent IS
    'Set to TRUE by the SMS cron job once a VACCINATION_REMINDER '
    'or APPOINTMENT notification is queued in sms_notifications.';
