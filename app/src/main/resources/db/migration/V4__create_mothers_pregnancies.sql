
-- MOTHERS TABLE

CREATE TABLE IF NOT EXISTS mothers (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID        NOT NULL,
    facility_id          UUID        NOT NULL,
    geo_location_id      UUID        NOT NULL,
    health_id            VARCHAR(32) NOT NULL,
    nida_verified_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    date_of_birth        DATE        NOT NULL,
    education_level      VARCHAR(32),
    registered_at        TIMESTAMP   NOT NULL DEFAULT NOW(),

    --  Constraints
    CONSTRAINT uq_mothers_user_id   UNIQUE (user_id),
    CONSTRAINT uq_mothers_health_id UNIQUE (health_id),

    CONSTRAINT chk_mothers_nida_status CHECK (
         nida_verified_status IN ('PENDING','VERIFIED','FAILED','MANUAL')
    ),

    CONSTRAINT chk_mothers_education CHECK (
         education_level IS NULL OR
         education_level IN ('NONE','PRIMARY','SECONDARY','TERTIARY')
    ),

    CONSTRAINT fk_mothers_user
      FOREIGN KEY (user_id)
       REFERENCES users (id)
       ON DELETE RESTRICT
       ON UPDATE CASCADE,

    CONSTRAINT fk_mothers_facility
       FOREIGN KEY (facility_id)
        REFERENCES facilities (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    CONSTRAINT fk_mothers_geo_location
        FOREIGN KEY (geo_location_id)
         REFERENCES geo_locations (id)
         ON DELETE RESTRICT
         ON UPDATE CASCADE
    );

--  Indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_mother_health_id
    ON mothers (health_id);

CREATE INDEX IF NOT EXISTS idx_mother_facility
    ON mothers (facility_id);

CREATE INDEX IF NOT EXISTS idx_mother_geo
    ON mothers (geo_location_id);

CREATE INDEX IF NOT EXISTS idx_mother_nida_status
    ON mothers (nida_verified_status);

CREATE INDEX IF NOT EXISTS idx_mother_dob
    ON mothers (date_of_birth);


-- HEALTH_ID SEQUENCE HELPER

CREATE SEQUENCE IF NOT EXISTS seq_mother_health_id
    START 1
    INCREMENT 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1;

-- PREGNANCIES TABLE

CREATE TABLE IF NOT EXISTS pregnancies (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mother_id       UUID        NOT NULL,
    lmp_date        DATE,
    edd             DATE,
    status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    gravida         INT,
    para            INT,
    assigned_chw_id UUID,
    outcome_notes   TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_pregnancy_status CHECK (
         status IN ('ACTIVE','DELIVERED','LOST','TRANSFERRED')
    ),

    CONSTRAINT chk_pregnancy_gravida CHECK (
         gravida IS NULL OR gravida >= 0
     ),

    CONSTRAINT chk_pregnancy_para CHECK (
         para IS NULL OR para >= 0
    ),

    CONSTRAINT chk_pregnancy_dates CHECK (
          lmp_date IS NULL OR edd IS NULL OR edd > lmp_date
    ),

    CONSTRAINT fk_pregnancies_mother
      FOREIGN KEY (mother_id)
       REFERENCES mothers (id)
       ON DELETE RESTRICT
       ON UPDATE CASCADE,

    CONSTRAINT fk_pregnancies_chw
       FOREIGN KEY (assigned_chw_id)
        REFERENCES users (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
    );

--Indexes
CREATE INDEX IF NOT EXISTS idx_pregnancy_mother
    ON pregnancies (mother_id);

CREATE INDEX IF NOT EXISTS idx_pregnancy_chw
    ON pregnancies (assigned_chw_id);

CREATE INDEX IF NOT EXISTS idx_pregnancy_status
    ON pregnancies (status);

CREATE INDEX IF NOT EXISTS idx_pregnancy_edd
    ON pregnancies (edd);

--
-- AUTO-UPDATE updated_at TRIGGER

CREATE OR REPLACE FUNCTION fn_set_updated_at()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$;

CREATE TRIGGER trg_pregnancies_updated_at
    BEFORE UPDATE ON pregnancies
    FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();

-- COMMENTS

COMMENT ON TABLE mothers IS
    'Core entity. NIDA cross-check sets geo_location_id '
    'automatically from national ID lookup. health_id is the '
    'shareable, printable identifier for mothers without smartphones.';

COMMENT ON COLUMN mothers.health_id IS
    'System-generated digital health ID. Format: MH-YYYY-#####. '
    'Printed on the mutuelles health card.';

COMMENT ON TABLE pregnancies IS
    'Separate entity — a mother can have multiple pregnancies '
    'over time. Never embed pregnancy data in the mothers row. '
    'Enables full obstetric history.';

