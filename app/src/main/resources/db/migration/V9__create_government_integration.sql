
-- 1. SERVICE_REQUESTS TABLE
CREATE TABLE IF NOT EXISTS service_requests (
     id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
     requester_id     UUID        NOT NULL,
     facility_id      UUID        NOT NULL,
     geo_location_id  UUID        NOT NULL,
     service_type     VARCHAR(32) NOT NULL,
     status           VARCHAR(24) NOT NULL DEFAULT 'PENDING',
     reference_no     VARCHAR(32) NOT NULL,
     irembo_ticket_id VARCHAR(64),
     payload          JSONB,
     rejection_reason TEXT,
     submitted_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    resolved_at      TIMESTAMP,
     resolved_by      UUID,

    -- Constraints
    CONSTRAINT uq_sr_reference_no UNIQUE (reference_no),

    CONSTRAINT chk_sr_service_type CHECK (
        service_type IN (
         'BIRTH_CERT',
         'VACCINATION_CARD',
         'REFERRAL',
         'HEALTH_SUMMARY',
         'REPRINT'
          )
      ),

   CONSTRAINT chk_sr_status CHECK (
      status IN (
         'PENDING',
         'UNDER_REVIEW',
         'APPROVED',
         'REJECTED',
         'IREMBO_SUBMITTED',
         'COMPLETED'
        )
     ),

   CONSTRAINT chk_sr_resolved CHECK (
       (resolved_at IS NULL AND resolved_by IS NULL) OR
        (resolved_at IS NOT NULL AND resolved_by IS NOT NULL)
           ),

   CONSTRAINT fk_sr_requester
     FOREIGN KEY (requester_id)
       REFERENCES users (id)
      ON DELETE RESTRICT
     ON UPDATE CASCADE,

   CONSTRAINT fk_sr_facility
     FOREIGN KEY (facility_id)
       REFERENCES facilities (id)
       ON DELETE RESTRICT
       ON UPDATE CASCADE,

   CONSTRAINT fk_sr_geo_location
     FOREIGN KEY (geo_location_id)
     REFERENCES geo_locations (id)
     ON DELETE RESTRICT
     ON UPDATE CASCADE,

   CONSTRAINT fk_sr_resolved_by
      FOREIGN KEY (resolved_by)
        REFERENCES users (id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

--  Indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_sr_ref
    ON service_requests (reference_no);

CREATE INDEX IF NOT EXISTS idx_sr_requester
    ON service_requests (requester_id);

CREATE INDEX IF NOT EXISTS idx_sr_facility
    ON service_requests (facility_id);

CREATE INDEX IF NOT EXISTS idx_sr_status
    ON service_requests (status);

CREATE INDEX IF NOT EXISTS idx_sr_geo
    ON service_requests (geo_location_id);

CREATE INDEX IF NOT EXISTS idx_sr_irembo
    ON service_requests (irembo_ticket_id)
    WHERE irembo_ticket_id IS NOT NULL;

-- GIN index on JSONB payload for service-specific field searches
CREATE INDEX IF NOT EXISTS idx_sr_payload
    ON service_requests USING GIN (payload jsonb_path_ops)
    WHERE payload IS NOT NULL;

--  REFERENCE NUMBER SEQUENCE

CREATE SEQUENCE IF NOT EXISTS seq_service_request_no
    START 1
    INCREMENT 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 1;


-- SERVICE_REQUEST_DOCS TABLE

CREATE TABLE IF NOT EXISTS service_request_docs (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id    UUID        NOT NULL,
    document_type VARCHAR(32) NOT NULL,
    file_path     VARCHAR(512) NOT NULL,
    file_hash     VARCHAR(64) NOT NULL,
     uploaded_at   TIMESTAMP   NOT NULL DEFAULT NOW(),

    --  Constraints
  CONSTRAINT chk_srd_doc_type CHECK (
       document_type IN (
         'ID_COPY',
         'BIRTH_PROOF',
         'FACILITY_LETTER',
         'OTHER'
          )
     ),

    -- SHA-256 is 64 hex chars
 CONSTRAINT chk_srd_file_hash CHECK (
    file_hash ~ '^[a-f0-9]{64}$'
      ),

  CONSTRAINT fk_srd_request
    FOREIGN KEY (request_id)
     REFERENCES service_requests (id)
     ON DELETE CASCADE
     ON UPDATE CASCADE
);

--  Indexes
CREATE INDEX IF NOT EXISTS idx_srd_request
    ON service_request_docs (request_id);

--  GOV_SYNC_LOG TABLE  (outbox pattern)

CREATE TABLE IF NOT EXISTS gov_sync_log (
      id   UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
      facility_id      UUID,
      target_system    VARCHAR(16) NOT NULL,
      sync_type        VARCHAR(32) NOT NULL,
     status           VARCHAR(16) NOT NULL DEFAULT 'PENDING',
     idempotency_key  VARCHAR(64) NOT NULL,
     payload_hash     VARCHAR(64),
     retry_count      INT         NOT NULL DEFAULT 0,
     error_message    TEXT,
     synced_at        TIMESTAMP,
     next_retry_at    TIMESTAMP,
    created_at       TIMESTAMP   NOT NULL DEFAULT NOW(),

    --  Constraints
  CONSTRAINT uq_gsync_idempotency UNIQUE (idempotency_key),

  CONSTRAINT chk_gsync_target CHECK (
    target_system IN ('NIDA','HMIS','IREMBO','RURA')
     ),

  CONSTRAINT chk_gsync_sync_type CHECK (
   sync_type IN (
       'IDENTITY_VERIFY',
       'REPORT_PUSH',
       'TICKET_SUBMIT',
       'SCHEDULE_PULL'
        )
   ),

  CONSTRAINT chk_gsync_status CHECK (
     status IN (
        'PENDING',
        'IN_FLIGHT',
        'SUCCEEDED',
        'FAILED',
        'DEAD_LETTER'
         )
     ),

   CONSTRAINT chk_gsync_retry CHECK (retry_count >= 0),

    CONSTRAINT fk_gsync_facility
      FOREIGN KEY (facility_id)
        REFERENCES facilities (id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

--  Indexes
CREATE UNIQUE INDEX IF NOT EXISTS idx_gsync_idempotency
    ON gov_sync_log (idempotency_key);

CREATE INDEX IF NOT EXISTS idx_gsync_status
    ON gov_sync_log (status);

CREATE INDEX IF NOT EXISTS idx_gsync_target
    ON gov_sync_log (target_system);

CREATE INDEX IF NOT EXISTS idx_gsync_retry
    ON gov_sync_log (next_retry_at)
    WHERE status IN ('PENDING','FAILED');


--  GOV_REPORTS TABLE

CREATE TABLE IF NOT EXISTS gov_reports (
      id  UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
      generated_by     UUID        NOT NULL,
     geo_location_id  UUID        NOT NULL,
     report_type      VARCHAR(32) NOT NULL,
     period           VARCHAR(16) NOT NULL,
     scope_level      VARCHAR(16) NOT NULL,
     aggregates       JSONB       NOT NULL,
     hmis_push_status VARCHAR(16) NOT NULL DEFAULT 'NOT_PUSHED',
     generated_at     TIMESTAMP   NOT NULL DEFAULT NOW(),
    pushed_at        TIMESTAMP,

    --  Constraints
  CONSTRAINT chk_greport_type CHECK (
     report_type IN (
        'VACCINATION_COVERAGE',
        'ANC_ATTENDANCE',
        'BIRTH_REGISTRATION',
        'MATERNAL_HEALTH'
         )
     ),

  CONSTRAINT chk_greport_scope CHECK (
     scope_level IN ('NATIONAL','PROVINCE','DISTRICT','SECTOR')
    ),

  CONSTRAINT chk_greport_hmis_status CHECK (
     hmis_push_status IN (
        'NOT_PUSHED',
        'QUEUED',
        'PUSHED',
        'FAILED'
       )
   ),

   CONSTRAINT fk_greport_generated_by
       FOREIGN KEY (generated_by)
       REFERENCES users (id)
       ON DELETE RESTRICT
       ON UPDATE CASCADE,

    CONSTRAINT fk_greport_geo_location
     FOREIGN KEY (geo_location_id)
     REFERENCES geo_locations (id)
     ON DELETE RESTRICT
     ON UPDATE CASCADE
);

--  Indexes
CREATE INDEX IF NOT EXISTS idx_greport_user
    ON gov_reports (generated_by);

CREATE INDEX IF NOT EXISTS idx_greport_geo
    ON gov_reports (geo_location_id);

CREATE INDEX IF NOT EXISTS idx_greport_type_period
    ON gov_reports (report_type, period, scope_level);

CREATE INDEX IF NOT EXISTS idx_greport_hmis
    ON gov_reports (hmis_push_status);

-- GIN index on aggregates JSONB for analytical queries
CREATE INDEX IF NOT EXISTS idx_greport_aggregates
    ON gov_reports USING GIN (aggregates jsonb_path_ops);

--  Comments
COMMENT ON TABLE service_requests IS
    'Citizens and CHWs submit government service requests here. '
        'Routed to FACILITY_ADMIN, optionally escalated to '
        'DISTRICT_OFFICER, then pushed async to Irembo portal '
        'via gov_sync_log outbox.';

COMMENT ON TABLE gov_sync_log IS
    'Outbox pattern implementation. Every government API call is '
        'written here FIRST, then executed by a background worker. '
        'Exponential backoff via next_retry_at. Status DEAD_LETTER '
        'after 5 retries — triggers admin alert.';

COMMENT ON TABLE gov_reports IS
    'Aggregated reports only — contains no individual patient '
        'records. aggregates JSONB holds computed stats '
        '(e.g. vaccination_coverage_pct, anc_visits_total). '
        'Pushed to MoH HMIS via gov_sync_log outbox.';

