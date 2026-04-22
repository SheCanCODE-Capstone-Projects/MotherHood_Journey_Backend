
-- HEALTH_VISITS TABLE

CREATE TABLE IF NOT EXISTS health_visits (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    patient_ref_id   UUID        NOT NULL,
    patient_type     VARCHAR(8)  NOT NULL,
    facility_id      UUID        NOT NULL,
    health_worker_id UUID        NOT NULL,
    geo_location_id  UUID,
    visit_datetime   TIMESTAMP   NOT NULL,
    visit_type       VARCHAR(32) NOT NULL,
    chief_complaint  TEXT,
    weight_kg        FLOAT,
    height_cm        FLOAT,
    systolic_bp      INT,
    diastolic_bp     INT,
    muac_cm          FLOAT,
    notes            TEXT,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_visit_patient_type CHECK (
       patient_type IN ('MOTHER','CHILD')
    ),

    CONSTRAINT chk_visit_type CHECK (
        visit_type IN (
        'ANC',
        'PNC',
        'IMMUNIZATION',
        'SICK_CHILD',
        'GROWTH_MONITORING'
        )
    ),

    CONSTRAINT chk_visit_bp CHECK (
(systolic_bp IS NULL AND diastolic_bp IS NULL)
    OR
(systolic_bp > 0 AND diastolic_bp > 0 AND systolic_bp > diastolic_bp)
    ),

    CONSTRAINT chk_visit_weight   CHECK (weight_kg  IS NULL OR weight_kg  > 0),
    CONSTRAINT chk_visit_height   CHECK (height_cm  IS NULL OR height_cm  > 0),
    CONSTRAINT chk_visit_muac     CHECK (muac_cm    IS NULL OR muac_cm    > 0),

    CONSTRAINT fk_visits_facility
    FOREIGN KEY (facility_id)
    REFERENCES facilities (id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT fk_visits_health_worker
    FOREIGN KEY (health_worker_id)
    REFERENCES users (id)
    ON DELETE RESTRICT
    ON UPDATE CASCADE,

    CONSTRAINT fk_visits_geo_location
    FOREIGN KEY (geo_location_id)
    REFERENCES geo_locations (id)
    ON DELETE SET NULL
    ON UPDATE CASCADE
    );

-- Indexes
CREATE INDEX IF NOT EXISTS idx_visit_patient
    ON health_visits (patient_ref_id);

CREATE INDEX IF NOT EXISTS idx_visit_facility
    ON health_visits (facility_id);

CREATE INDEX IF NOT EXISTS idx_visit_datetime
    ON health_visits (visit_datetime DESC);

CREATE INDEX IF NOT EXISTS idx_visit_worker
    ON health_visits (health_worker_id);

CREATE INDEX IF NOT EXISTS idx_visit_patient_poly
    ON health_visits (patient_ref_id, patient_type);

CREATE INDEX IF NOT EXISTS idx_visit_type
    ON health_visits (visit_type);


--  DIAGNOSES TABLE

CREATE TABLE IF NOT EXISTS diagnoses (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    visit_id    UUID        NOT NULL,
    icd10_code  VARCHAR(16) NOT NULL,
    description VARCHAR(255) NOT NULL,
    severity    VARCHAR(16),
    is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT chk_diag_severity CHECK (
       severity IS NULL OR
       severity IN ('MILD','MODERATE','SEVERE')
    ),

    -- ICD-10 codes: letter + 2 digits, optional dot + 1-2 chars
    CONSTRAINT chk_diag_icd10 CHECK (
       icd10_code ~ '^[A-Z][0-9]{2}(\.[0-9A-Z]{1,2})?$'
             ),

    CONSTRAINT fk_diagnoses_visit
    FOREIGN KEY (visit_id)
    REFERENCES health_visits (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
    );

--  Indexes
CREATE INDEX IF NOT EXISTS idx_diag_visit
    ON diagnoses (visit_id);

CREATE INDEX IF NOT EXISTS idx_diag_icd10
    ON diagnoses (icd10_code);

-- Partial index — quick lookup of primary diagnosis per visit
CREATE INDEX IF NOT EXISTS idx_diag_primary
    ON diagnoses (visit_id)
    WHERE is_primary = TRUE;

--  PRESCRIPTIONS TABLE

CREATE TABLE IF NOT EXISTS prescriptions (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    visit_id        UUID        NOT NULL,
    medication_name VARCHAR(64) NOT NULL,
    dosage          VARCHAR(64) NOT NULL,
    frequency       VARCHAR(64) NOT NULL,
    duration_days   INT         NOT NULL,
    instructions    TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),

    --  Constraints
    CONSTRAINT chk_rx_duration CHECK (duration_days > 0),

    CONSTRAINT fk_prescriptions_visit
    FOREIGN KEY (visit_id)
    REFERENCES health_visits (id)
    ON DELETE CASCADE
    ON UPDATE CASCADE
    );

-- Indexes
CREATE INDEX IF NOT EXISTS idx_rx_visit
    ON prescriptions (visit_id);

CREATE INDEX IF NOT EXISTS idx_rx_medication
    ON prescriptions (medication_name);


-- COMMENTS

COMMENT ON TABLE health_visits IS
    'Polymorphic patient reference supports both mother and child '
    'visits from one table. visit_type covers ANC (antenatal), '
    'PNC (postnatal), and growth monitoring — all key maternal '
    'health touchpoints.';

COMMENT ON COLUMN health_visits.patient_ref_id IS
    'Polymorphic FK — references mothers.id when patient_type=MOTHER '
    'or children.id when patient_type=CHILD. No DB-level FK enforced; '
    'application layer validates. Use idx_visit_patient_poly.';

COMMENT ON COLUMN health_visits.muac_cm IS
    'Mid-upper arm circumference. Primary malnutrition screening '
    'metric used by CHWs for children under 5.';

COMMENT ON TABLE diagnoses IS
    'Multiple diagnoses per visit. icd10_code subset is synced from '
    'MoH HMIS on schedule updates. is_primary marks the presenting '
    'condition for analytics aggregation.';

COMMENT ON TABLE prescriptions IS
    'Medications issued per visit. Forms the basis of medication '
    'adherence SMS reminders — sms_notifications are scheduled at '
    'duration_days intervals.';

