
-- TABLE: facilities

CREATE TABLE IF NOT EXISTS facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    geo_location_id UUID  NOT NULL REFERENCES geo_locations(id),
    name            VARCHAR(128)  NOT NULL,
    facility_code   VARCHAR(32)   NOT NULL,
    facility_type   VARCHAR(32)   NOT NULL,
    district        VARCHAR(64)   NOT NULL,
    phone           VARCHAR(20),
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT uq_facility_code UNIQUE (facility_code),

    CONSTRAINT chk_facility_type CHECK (
         facility_type IN (
           'HEALTH_CENTER',
           'HOSPITAL',
           'CLINIC',
           'CHW_POST'
          )
       )
    );

COMMENT ON TABLE  facilities IS 'Every patient row, visit row, and service request carries a facility_id. This is the primary multi-tenancy boundary — a HEALTH_WORKER at Kacyiru Health Center never sees data from Remera Health Center.';
COMMENT ON COLUMN facilities.facility_code IS 'Rwanda MoH official facility code — unique across all facilities nationally';
COMMENT ON COLUMN facilities.district      IS 'Denormalized copy of district from geo_locations — avoids join on every list/filter query';
COMMENT ON COLUMN facilities.facility_type IS 'HEALTH_CENTER | HOSPITAL | CLINIC | CHW_POST';

-- Indexes

CREATE UNIQUE INDEX IF NOT EXISTS idx_facility_code
    ON facilities (facility_code);

CREATE INDEX IF NOT EXISTS idx_facility_geo
    ON facilities (geo_location_id);

CREATE INDEX IF NOT EXISTS idx_facility_district
    ON facilities (district);

CREATE INDEX IF NOT EXISTS idx_facility_type
    ON facilities (facility_type);

-- Partial index: active facilities only
CREATE INDEX IF NOT EXISTS idx_facility_active
    ON facilities (district, facility_type) WHERE active = TRUE;

