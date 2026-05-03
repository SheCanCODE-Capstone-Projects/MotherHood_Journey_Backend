-- ============================================================
-- V1__Initial_Schema.sql
-- MotherHood Journey — Full Database Schema
-- IgireRwanda Organization | SheCanCode Bootcamp | Kigali, Rwanda
-- ============================================================

-- ============================================================
-- GROUP 1: GEO-IDENTITY & ADMINISTRATIVE
-- ============================================================
CREATE TABLE IF NOT EXISTS geo_locations (
                                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             province        VARCHAR(64)  NOT NULL,
                                             district        VARCHAR(64)  NOT NULL,
                                             sector          VARCHAR(64)  NOT NULL,
                                             cell            VARCHAR(64)  NOT NULL,
                                             village         VARCHAR(64)  NOT NULL,
                                             postal_code     VARCHAR(16),
                                             latitude        FLOAT,
                                             longitude       FLOAT,
                                             active          BOOLEAN      NOT NULL DEFAULT TRUE,
                                             created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_geo_pds    ON geo_locations (province, district, sector);
CREATE INDEX IF NOT EXISTS idx_geo_sector ON geo_locations (sector);

-- ============================================================
-- GROUP 2: FACILITIES
-- ============================================================
CREATE TABLE IF NOT EXISTS facilities (
                                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          geo_location_id UUID         NOT NULL REFERENCES geo_locations (id),
                                          name            VARCHAR(128) NOT NULL,
                                          facility_code   VARCHAR(32)  NOT NULL UNIQUE,
                                          facility_type   VARCHAR(32)  NOT NULL,
                                          district        VARCHAR(64)  NOT NULL,
                                          phone           VARCHAR(20),
                                          active          BOOLEAN      NOT NULL DEFAULT TRUE,
                                          created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_facility_code ON facilities (facility_code);
CREATE INDEX IF NOT EXISTS        idx_facility_geo  ON facilities (geo_location_id);

-- ============================================================
-- GROUP 3: USERS
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
                                     id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     facility_id        UUID         REFERENCES facilities (id),
                                     geo_location_id    UUID         NOT NULL REFERENCES geo_locations (id),
                                     national_id        VARCHAR(32)  NOT NULL UNIQUE,
                                     phone_number       VARCHAR(20)  NOT NULL UNIQUE,
                                     password_hash      VARCHAR(255) NOT NULL,
                                     role               VARCHAR(32)  NOT NULL,
                                     first_name         VARCHAR(64)  NOT NULL,
                                     last_name          VARCHAR(64)  NOT NULL,
                                     preferred_language VARCHAR(8)   NOT NULL DEFAULT 'rw',
                                     active             BOOLEAN      NOT NULL DEFAULT TRUE,
                                     created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     last_login         TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_nid      ON users (national_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone    ON users (phone_number);
CREATE INDEX IF NOT EXISTS        idx_users_role     ON users (role);
CREATE INDEX IF NOT EXISTS        idx_users_facility ON users (facility_id);

-- ============================================================
-- GROUP 4: GOVERNMENT USERS (1:1 extension of users)
-- ============================================================
CREATE TABLE IF NOT EXISTS government_users (
                                                id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                user_id        UUID        NOT NULL UNIQUE REFERENCES users (id),
                                                gov_role       VARCHAR(32) NOT NULL,
                                                ministry       VARCHAR(64) NOT NULL,
                                                employee_id    VARCHAR(64) NOT NULL UNIQUE,
                                                scoped_geo_ids UUID[],
                                                can_export     BOOLEAN     NOT NULL DEFAULT FALSE,
                                                can_push_hmis  BOOLEAN     NOT NULL DEFAULT FALSE,
                                                last_audit     TIMESTAMP,
                                                created_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- GROUP 5: MOTHERS & PREGNANCIES
-- ============================================================
CREATE TABLE IF NOT EXISTS mothers (
                                       id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       user_id             UUID        NOT NULL UNIQUE REFERENCES users (id),
                                       facility_id         UUID        NOT NULL REFERENCES facilities (id),
                                       geo_location_id     UUID        NOT NULL REFERENCES geo_locations (id),
                                       health_id           VARCHAR(32) NOT NULL UNIQUE,
                                       nida_verified_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                                       date_of_birth       DATE        NOT NULL,
                                       education_level     VARCHAR(32),
                                       registered_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mother_health_id   ON mothers (health_id);
CREATE INDEX IF NOT EXISTS        idx_mother_facility    ON mothers (facility_id);
CREATE INDEX IF NOT EXISTS        idx_mother_geo         ON mothers (geo_location_id);
CREATE INDEX IF NOT EXISTS        idx_mother_nida_status ON mothers (nida_verified_status);

CREATE TABLE IF NOT EXISTS pregnancies (
                                           id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           mother_id       UUID        NOT NULL REFERENCES mothers (id),
                                           lmp_date        DATE,
                                           edd             DATE,
                                           status          VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
                                           gravida         INT,
                                           para            INT,
                                           assigned_chw_id UUID        REFERENCES users (id),
                                           outcome_notes   TEXT,
                                           created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pregnancy_mother ON pregnancies (mother_id);
CREATE INDEX IF NOT EXISTS idx_pregnancy_chw    ON pregnancies (assigned_chw_id);
CREATE INDEX IF NOT EXISTS idx_pregnancy_status ON pregnancies (status);

-- ============================================================
-- GROUP 6: CHILDREN & VACCINATION
-- ============================================================
CREATE TABLE IF NOT EXISTS children (
                                        id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                        mother_id            UUID        NOT NULL REFERENCES mothers (id),
                                        facility_id          UUID        NOT NULL REFERENCES facilities (id),
                                        geo_location_id      UUID        NOT NULL REFERENCES geo_locations (id),
                                        birth_certificate_no VARCHAR(64) UNIQUE,
                                        first_name           VARCHAR(64),
                                        gender               VARCHAR(8),
                                        date_of_birth        DATE        NOT NULL,
                                        birth_weight_kg      FLOAT,
                                        delivery_type        VARCHAR(16),
                                        health_status        VARCHAR(16) NOT NULL DEFAULT 'HEALTHY',
                                        registered_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS        idx_child_mother      ON children (mother_id);
CREATE INDEX IF NOT EXISTS        idx_child_facility    ON children (facility_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_child_birth_cert  ON children (birth_certificate_no);
CREATE INDEX IF NOT EXISTS        idx_child_health_status ON children (health_status);

CREATE TABLE IF NOT EXISTS vaccination_schedules (
                                                     id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                     vaccine_name VARCHAR(64) NOT NULL,
                                                     antigen_code VARCHAR(16) NOT NULL UNIQUE,
                                                     dose_number  INT         NOT NULL,
                                                     due_age_days INT         NOT NULL,
                                                     window_days  INT         NOT NULL DEFAULT 7,
                                                     is_mandatory BOOLEAN     NOT NULL DEFAULT TRUE,
                                                     description  TEXT,
                                                     updated_at   TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_vacc_sched_code ON vaccination_schedules (antigen_code);

CREATE TABLE IF NOT EXISTS vaccination_records (
                                                   id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                   child_id          UUID        NOT NULL REFERENCES children (id),
                                                   schedule_id       UUID        NOT NULL REFERENCES vaccination_schedules (id),
                                                   administered_by   UUID        REFERENCES users (id),
                                                   facility_id       UUID        NOT NULL REFERENCES facilities (id),
                                                   administered_date DATE,
                                                   due_date          DATE        NOT NULL,
                                                   lot_number        VARCHAR(32),
                                                   status            VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                                                   notes             TEXT,
                                                   created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                   UNIQUE (child_id, schedule_id)
);

CREATE INDEX IF NOT EXISTS idx_vacc_rec_child  ON vaccination_records (child_id);
CREATE INDEX IF NOT EXISTS idx_vacc_rec_status ON vaccination_records (status);
CREATE INDEX IF NOT EXISTS idx_vacc_rec_due    ON vaccination_records (due_date);

-- ============================================================
-- GROUP 7: CLINICAL VISITS
-- ============================================================
CREATE TABLE IF NOT EXISTS health_visits (
                                             id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             patient_ref_id   UUID        NOT NULL,
                                             patient_type     VARCHAR(8)  NOT NULL,
                                             facility_id      UUID        NOT NULL REFERENCES facilities (id),
                                             health_worker_id UUID        NOT NULL REFERENCES users (id),
                                             geo_location_id  UUID        REFERENCES geo_locations (id),
                                             visit_datetime   TIMESTAMP   NOT NULL,
                                             visit_type       VARCHAR(16) NOT NULL,
                                             chief_complaint  TEXT,
                                             weight_kg        FLOAT,
                                             height_cm        FLOAT,
                                             systolic_bp      INT,
                                             diastolic_bp     INT,
                                             muac_cm          FLOAT,
                                             notes            TEXT,
                                             created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visit_patient     ON health_visits (patient_ref_id);
CREATE INDEX IF NOT EXISTS idx_visit_facility    ON health_visits (facility_id);
CREATE INDEX IF NOT EXISTS idx_visit_datetime    ON health_visits (visit_datetime);
CREATE INDEX IF NOT EXISTS idx_visit_worker      ON health_visits (health_worker_id);
CREATE INDEX IF NOT EXISTS idx_visit_patient_poly ON health_visits (patient_ref_id, patient_type);

CREATE TABLE IF NOT EXISTS diagnoses (
                                         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         visit_id    UUID        NOT NULL REFERENCES health_visits (id),
                                         icd10_code  VARCHAR(8)  NOT NULL,
                                         description VARCHAR(255) NOT NULL,
                                         severity    VARCHAR(16),
                                         is_primary  BOOLEAN     NOT NULL DEFAULT FALSE,
                                         created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_diag_visit  ON diagnoses (visit_id);
CREATE INDEX IF NOT EXISTS idx_diag_icd10 ON diagnoses (icd10_code);

CREATE TABLE IF NOT EXISTS prescriptions (
                                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             visit_id        UUID        NOT NULL REFERENCES health_visits (id),
                                             medication_name VARCHAR(64) NOT NULL,
                                             dosage          VARCHAR(64) NOT NULL,
                                             frequency       VARCHAR(64) NOT NULL,
                                             duration_days   INT         NOT NULL,
                                             instructions    TEXT,
                                             created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rx_visit ON prescriptions (visit_id);

-- ============================================================
-- GROUP 8: APPOINTMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS appointments (
                                            id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            patient_ref_id   UUID        NOT NULL,
                                            patient_type     VARCHAR(8)  NOT NULL,
                                            facility_id      UUID        NOT NULL REFERENCES facilities (id),
                                            health_worker_id UUID        REFERENCES users (id),
                                            geo_location_id  UUID        REFERENCES geo_locations (id),
                                            scheduled_at     TIMESTAMP   NOT NULL,
                                            appointment_type VARCHAR(32) NOT NULL,
                                            status           VARCHAR(16) NOT NULL DEFAULT 'SCHEDULED',
                                            reminder_sent    BOOLEAN     NOT NULL DEFAULT FALSE,
                                            notes            TEXT,
                                            created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_appt_patient  ON appointments (patient_ref_id);
CREATE INDEX IF NOT EXISTS idx_appt_facility ON appointments (facility_id);
CREATE INDEX IF NOT EXISTS idx_appt_datetime ON appointments (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_appt_status   ON appointments (status);

-- ============================================================
-- GROUP 9: CONSENT
-- ============================================================
CREATE TABLE IF NOT EXISTS consent_records (
                                               id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               mother_id      UUID        NOT NULL REFERENCES mothers (id),
                                               consent_type   VARCHAR(32) NOT NULL,
                                               granted        BOOLEAN     NOT NULL,
                                               granted_by_role VARCHAR(32),
                                               consented_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                               expires_at     TIMESTAMP,
                                               legal_basis    VARCHAR(32),
                                               revoked_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_consent_mother  ON consent_records (mother_id);
CREATE INDEX IF NOT EXISTS idx_consent_type    ON consent_records (mother_id, consent_type);
CREATE INDEX IF NOT EXISTS idx_consent_expiry  ON consent_records (expires_at);

-- ============================================================
-- GROUP 10: GOVERNMENT INTEGRATION
-- ============================================================
CREATE TABLE IF NOT EXISTS service_requests (
                                                id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                requester_id     UUID        NOT NULL REFERENCES users (id),
                                                facility_id      UUID        NOT NULL REFERENCES facilities (id),
                                                geo_location_id  UUID        NOT NULL REFERENCES geo_locations (id),
                                                service_type     VARCHAR(32) NOT NULL,
                                                status           VARCHAR(24) NOT NULL DEFAULT 'PENDING',
                                                reference_no     VARCHAR(32) NOT NULL UNIQUE,
                                                irembo_ticket_id VARCHAR(64),
                                                payload          JSONB,
                                                rejection_reason TEXT,
                                                submitted_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                                resolved_at      TIMESTAMP,
                                                resolved_by      UUID        REFERENCES users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_sr_ref       ON service_requests (reference_no);
CREATE INDEX IF NOT EXISTS        idx_sr_requester ON service_requests (requester_id);
CREATE INDEX IF NOT EXISTS        idx_sr_facility  ON service_requests (facility_id);
CREATE INDEX IF NOT EXISTS        idx_sr_status    ON service_requests (status);
CREATE INDEX IF NOT EXISTS        idx_sr_geo       ON service_requests (geo_location_id);
CREATE INDEX IF NOT EXISTS        idx_sr_irembo    ON service_requests (irembo_ticket_id);

CREATE TABLE IF NOT EXISTS service_request_docs (
                                                    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                    request_id    UUID        NOT NULL REFERENCES service_requests (id),
                                                    document_type VARCHAR(32) NOT NULL,
                                                    file_path     VARCHAR(255) NOT NULL,
                                                    file_hash     VARCHAR(64) NOT NULL,
                                                    uploaded_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_srd_request ON service_request_docs (request_id);

CREATE TABLE IF NOT EXISTS gov_sync_log (
                                            id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            facility_id      UUID        REFERENCES facilities (id),
                                            target_system    VARCHAR(16) NOT NULL,
                                            sync_type        VARCHAR(32) NOT NULL,
                                            status           VARCHAR(16) NOT NULL DEFAULT 'PENDING',
                                            idempotency_key  VARCHAR(64) NOT NULL UNIQUE,
                                            payload_hash     VARCHAR(64),
                                            retry_count      INT         NOT NULL DEFAULT 0,
                                            error_message    TEXT,
                                            synced_at        TIMESTAMP,
                                            next_retry_at    TIMESTAMP,
                                            created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_gsync_idempotency ON gov_sync_log (idempotency_key);
CREATE INDEX IF NOT EXISTS        idx_gsync_status      ON gov_sync_log (status);
CREATE INDEX IF NOT EXISTS        idx_gsync_target      ON gov_sync_log (target_system);
CREATE INDEX IF NOT EXISTS        idx_gsync_retry       ON gov_sync_log (next_retry_at);

CREATE TABLE IF NOT EXISTS gov_reports (
                                           id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                           generated_by     UUID        NOT NULL REFERENCES users (id),
                                           geo_location_id  UUID        NOT NULL REFERENCES geo_locations (id),
                                           report_type      VARCHAR(32) NOT NULL,
                                           period           VARCHAR(16) NOT NULL,
                                           scope_level      VARCHAR(16) NOT NULL,
                                           aggregates       JSONB       NOT NULL,
                                           hmis_push_status VARCHAR(16) NOT NULL DEFAULT 'NOT_PUSHED',
                                           generated_at     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           pushed_at        TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_greport_user        ON gov_reports (generated_by);
CREATE INDEX IF NOT EXISTS idx_greport_geo         ON gov_reports (geo_location_id);
CREATE INDEX IF NOT EXISTS idx_greport_type_period ON gov_reports (report_type, period, scope_level);
CREATE INDEX IF NOT EXISTS idx_greport_hmis        ON gov_reports (hmis_push_status);

-- ============================================================
-- GROUP 11: NOTIFICATIONS & AUDIT
-- ============================================================
CREATE TABLE IF NOT EXISTS sms_notifications (
                                                 id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                 recipient_user_id UUID        NOT NULL REFERENCES users (id),
                                                 phone_number      VARCHAR(20) NOT NULL,
                                                 message_body      TEXT        NOT NULL,
                                                 notification_type VARCHAR(32) NOT NULL,
                                                 status            VARCHAR(16) NOT NULL DEFAULT 'QUEUED',
                                                 at_message_id     VARCHAR(64),
                                                 scheduled_at      TIMESTAMP   NOT NULL,
                                                 sent_at           TIMESTAMP,
                                                 retry_count       INT         NOT NULL DEFAULT 0,
                                                 created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_sms_user      ON sms_notifications (recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_sms_status    ON sms_notifications (status);
CREATE INDEX IF NOT EXISTS idx_sms_scheduled ON sms_notifications (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_sms_type      ON sms_notifications (notification_type);

CREATE TABLE IF NOT EXISTS audit_log (
                                         id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         user_id       UUID         NOT NULL REFERENCES users (id),
                                         action        VARCHAR(32)  NOT NULL,
                                         resource_type VARCHAR(32)  NOT NULL,
                                         resource_id   UUID,
                                         geo_location_id UUID       REFERENCES geo_locations (id),
                                         ip_address    VARCHAR(45),
                                         user_agent    VARCHAR(255),
                                         success       BOOLEAN      NOT NULL DEFAULT TRUE,
                                         fail_reason   VARCHAR(128),
                                         created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_user        ON audit_log (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_resource    ON audit_log (resource_type);
CREATE INDEX IF NOT EXISTS idx_audit_resource_id ON audit_log (resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_ts          ON audit_log (created_at);