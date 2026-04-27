
--  CHILDREN TABLE

CREATE TABLE IF NOT EXISTS children (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mother_id            UUID        NOT NULL,
    facility_id          UUID        NOT NULL,
    geo_location_id      UUID        NOT NULL,
    birth_certificate_no VARCHAR(64),
    first_name           VARCHAR(64),
    gender               VARCHAR(8)  NOT NULL DEFAULT 'UNKNOWN',
    date_of_birth        DATE        NOT NULL,
    birth_weight_kg      FLOAT,
    delivery_type        VARCHAR(16),
    health_status        VARCHAR(16) NOT NULL DEFAULT 'HEALTHY',
    registered_at        TIMESTAMP   NOT NULL DEFAULT NOW(),

    --  Constraints
    CONSTRAINT uq_children_birth_cert UNIQUE (birth_certificate_no),

    CONSTRAINT chk_children_gender CHECK (
        gender IN ('MALE','FEMALE','UNKNOWN')
    ),

    CONSTRAINT chk_children_delivery CHECK (
       delivery_type IS NULL OR
       delivery_type IN ('NORMAL','CAESAREAN','ASSISTED')
    ),

    CONSTRAINT chk_children_health_status CHECK (
       health_status IN ('HEALTHY','AT_RISK','CRITICAL')
    ),

    CONSTRAINT chk_children_birth_weight CHECK (
       birth_weight_kg IS NULL OR birth_weight_kg > 0
    ),

    CONSTRAINT fk_children_mother
      FOREIGN KEY (mother_id)
        REFERENCES mothers (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_children_facility
      FOREIGN KEY (facility_id)
        REFERENCES facilities (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_children_geo_location
     FOREIGN KEY (geo_location_id)
        REFERENCES geo_locations (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
    );

--  Indexes
CREATE INDEX IF NOT EXISTS idx_child_mother
    ON children (mother_id);

CREATE INDEX IF NOT EXISTS idx_child_facility
    ON children (facility_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_child_birth_cert
    ON children (birth_certificate_no)
    WHERE birth_certificate_no IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_child_status
    ON children (health_status);

CREATE INDEX IF NOT EXISTS idx_child_dob
    ON children (date_of_birth);

--  VACCINATION_SCHEDULES TABLE
CREATE TABLE IF NOT EXISTS vaccination_schedules (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    vaccine_name  VARCHAR(64) NOT NULL,
    antigen_code  VARCHAR(16) NOT NULL,
    dose_number   INT         NOT NULL,
    due_age_days  INT         NOT NULL,
    window_days   INT         NOT NULL DEFAULT 7,
    is_mandatory  BOOLEAN     NOT NULL DEFAULT TRUE,
    description   TEXT,
    updated_at    TIMESTAMP,

    --  Constraints
    CONSTRAINT uq_vacc_sched_code UNIQUE (antigen_code),

    CONSTRAINT chk_vacc_sched_dose CHECK (dose_number >= 1),
    CONSTRAINT chk_vacc_sched_age  CHECK (due_age_days >= 0),
    CONSTRAINT chk_vacc_sched_window CHECK (window_days >= 0)
    );

--  Index
CREATE UNIQUE INDEX IF NOT EXISTS idx_vacc_sched_code
    ON vaccination_schedules (antigen_code);


-- VACCINATION_RECORDS TABLE

CREATE TABLE IF NOT EXISTS vaccination_records (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id          UUID        NOT NULL,
    schedule_id       UUID        NOT NULL,
    administered_by   UUID,
    facility_id       UUID        NOT NULL,
    administered_date DATE,
    due_date          DATE        NOT NULL,
    lot_number        VARCHAR(32),
    status            VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    notes             TEXT,
    created_at        TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_vacc_rec_child_sched UNIQUE (child_id, schedule_id),

    CONSTRAINT chk_vacc_rec_status CHECK (
        status IN ('PENDING','ADMINISTERED','MISSED','OVERDUE')
    ),

    CONSTRAINT fk_vacc_rec_child
      FOREIGN KEY (child_id)
       REFERENCES children (id)
       ON DELETE CASCADE
       ON UPDATE CASCADE,

    CONSTRAINT fk_vacc_rec_schedule
       FOREIGN KEY (schedule_id)
         REFERENCES vaccination_schedules (id)
         ON DELETE RESTRICT
         ON UPDATE CASCADE,

    CONSTRAINT fk_vacc_rec_administered_by
      FOREIGN KEY (administered_by)
       REFERENCES users (id)
       ON DELETE SET NULL
       ON UPDATE CASCADE,

    CONSTRAINT fk_vacc_rec_facility
       FOREIGN KEY (facility_id)
        REFERENCES facilities (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
       );

-- Indexes
CREATE INDEX IF NOT EXISTS idx_vacc_rec_child
    ON vaccination_records (child_id);

CREATE INDEX IF NOT EXISTS idx_vacc_rec_status
    ON vaccination_records (status);

CREATE INDEX IF NOT EXISTS idx_vacc_rec_due
    ON vaccination_records (due_date);

CREATE INDEX IF NOT EXISTS idx_vacc_rec_facility
    ON vaccination_records (facility_id);

-- Partial index for cron job: scan only PENDING past window
CREATE INDEX IF NOT EXISTS idx_vacc_rec_pending
    ON vaccination_records (due_date)
    WHERE status = 'PENDING';

--  AUTO-SCHEDULE TRIGGER

CREATE OR REPLACE FUNCTION fn_create_vaccination_records()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
INSERT INTO vaccination_records
(child_id, schedule_id, facility_id, due_date, status)
SELECT
    NEW.id,
    vs.id,
    NEW.facility_id,
    NEW.date_of_birth + vs.due_age_days,
    'PENDING'
FROM vaccination_schedules vs
WHERE vs.is_mandatory = TRUE
    ON CONFLICT (child_id, schedule_id) DO NOTHING;

RETURN NEW;
END;
$$;

CREATE TRIGGER trg_child_create_vacc_records
    AFTER INSERT ON children
    FOR EACH ROW EXECUTE FUNCTION fn_create_vaccination_records();


--  COMMENTS

COMMENT ON TABLE children IS
    'Registered at birth. birth_certificate_no replaces the paper '
    'birth record — this is the digital twin of the physical mutuelles '
    'card. health_status is updated by CHW visits.';

COMMENT ON TABLE vaccination_schedules IS
    'Seeded via Flyway with full Rwanda EPI schedule. Updated when '
    'MoH HMIS pushes schedule changes. Never deleted — '
    'soft-deprecated with is_mandatory = FALSE.';

COMMENT ON TABLE vaccination_records IS
    'Created automatically via trigger when a child is registered — '
    'one row per mandatory EPI schedule entry. A cron job scans PENDING '
    'records past due_date + window_days and flips to OVERDUE, '
    'triggering an SMS via sms_notifications.';

