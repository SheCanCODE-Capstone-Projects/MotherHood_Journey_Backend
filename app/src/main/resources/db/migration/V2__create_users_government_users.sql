
CREATE EXTENSION IF NOT EXISTS pgcrypto;
-- TABLE:users
CREATE TABLE IF NOT EXISTS users (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      facility_id  UUID REFERENCES facilities(id),
      geo_location_id UUID NOT NULL REFERENCES geo_locations(id),
      national_id   VARCHAR(32)   NOT NULL,
      phone_number  VARCHAR(20)   NOT NULL,
      password_hash  VARCHAR(255)  NOT NULL,
      role  VARCHAR(32)   NOT NULL,
      first_name  VARCHAR(64)   NOT NULL,
      last_name   VARCHAR(64)   NOT NULL,
      preferred_language  VARCHAR(8)    NOT NULL DEFAULT 'rw',
      active  BOOLEAN   NOT NULL DEFAULT TRUE,
      created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
      last_login  TIMESTAMP,

    --  Constraints
     CONSTRAINT uq_users_national_id   UNIQUE (national_id),
    CONSTRAINT uq_users_phone_number  UNIQUE (phone_number),

    -- Valid roles defined in the schema document
    CONSTRAINT chk_users_role CHECK (
    role IN (
       'PATIENT',
        'HEALTH_WORKER',
        'FACILITY_ADMIN',
        'DISTRICT_OFFICER',
        'GOVERNMENT_ANALYST',
        'MOH_ADMIN'
        )
     ),

    -- Valid language codes
    CONSTRAINT chk_users_language CHECK (
         preferred_language IN ('rw', 'en', 'fr')
    )
);

COMMENT ON TABLE  users IS 'All six roles share this single table. Government roles additionally have a government_users row (1:1). Enables unified JWT auth pipeline.';
COMMENT ON COLUMN users.role            IS 'PATIENT | HEALTH_WORKER | FACILITY_ADMIN | DISTRICT_OFFICER | GOVERNMENT_ANALYST | MOH_ADMIN';
COMMENT ON COLUMN users.national_id     IS 'Rwanda NID — verified by NIDA';
COMMENT ON COLUMN users.facility_id     IS 'Home facility — NULL allowed for government staff';
COMMENT ON COLUMN users.geo_location_id IS 'Village-level location — cross-checked against NIDA';

-- Indexes

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_nid
    ON users (national_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone
    ON users (phone_number);

CREATE INDEX IF NOT EXISTS idx_users_role
    ON users (role);

CREATE INDEX IF NOT EXISTS idx_users_facility
    ON users (facility_id);

-- Partial index: quickly find active users by role (common dashboard query)
CREATE INDEX IF NOT EXISTS idx_users_active_role
    ON users (role) WHERE active = TRUE;

-- TABLE: government_users

CREATE TABLE IF NOT EXISTS government_users (
     id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
     user_id         UUID          NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
     gov_role        VARCHAR(32)   NOT NULL,
     ministry        VARCHAR(128)  NOT NULL,
     employee_id     VARCHAR(64)   NOT NULL,
     scoped_geo_ids  UUID[]        DEFAULT '{}',
     can_export      BOOLEAN       NOT NULL DEFAULT FALSE,
     can_push_hmis   BOOLEAN       NOT NULL DEFAULT FALSE,
     last_audit      TIMESTAMP,
     created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),

    --  Constraints
      CONSTRAINT uq_gov_users_employee_id UNIQUE (employee_id),

     CONSTRAINT chk_gov_role CHECK (
        gov_role IN (
           'DISTRICT_OFFICER',
            'GOVERNMENT_ANALYST',
             'MOH_ADMIN'
                 )
             )
);

COMMENT ON TABLE  government_users IS '1:1 extension of users. Government staff log in via the same JWT pipeline. scoped_geo_ids drives sector-level RBAC — Spring @PreAuthorize checks resource geo_location_id against this array.';
COMMENT ON COLUMN government_users.scoped_geo_ids IS 'PostgreSQL UUID array — authorized sector/district geo_location UUIDs for DISTRICT_OFFICER. Empty array = no geographic restriction (used for MOH_ADMIN / GOVERNMENT_ANALYST).';
COMMENT ON COLUMN government_users.can_export     IS 'Permission to export CSV/reports';
COMMENT ON COLUMN government_users.can_push_hmis  IS 'Permission to push data to MoH HMIS';
COMMENT ON COLUMN government_users.last_audit     IS 'Last access-audit review timestamp';

-- ── Indexes ──────────────────────────────────────────────

CREATE UNIQUE INDEX IF NOT EXISTS idx_gov_users_user_id
    ON government_users (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_gov_users_employee_id
    ON government_users (employee_id);

CREATE INDEX IF NOT EXISTS idx_gov_users_role
    ON government_users (gov_role);

CREATE INDEX IF NOT EXISTS idx_gov_users_scoped_geo_gin
    ON government_users USING GIN (scoped_geo_ids);

CREATE OR REPLACE FUNCTION trg_validate_gov_user_role()
    RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    parent_role VARCHAR(32);
BEGIN
    SELECT role INTO parent_role
    FROM users
    WHERE id = NEW.user_id;

    IF parent_role NOT IN ('DISTRICT_OFFICER', 'GOVERNMENT_ANALYST', 'MOH_ADMIN') THEN
        RAISE EXCEPTION
            'government_users row rejected: users.role is "%" — must be DISTRICT_OFFICER, GOVERNMENT_ANALYST, or MOH_ADMIN',
            parent_role;
    END IF;

    -- Also ensure gov_role matches parent role exactly
    IF NEW.gov_role <> parent_role THEN
        RAISE EXCEPTION
            'government_users.gov_role "%" must match users.role "%"',
            NEW.gov_role, parent_role;
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_gov_user_role_check ON government_users;
CREATE TRIGGER trg_gov_user_role_check
    BEFORE INSERT OR UPDATE ON government_users
    FOR EACH ROW EXECUTE FUNCTION trg_validate_gov_user_role();