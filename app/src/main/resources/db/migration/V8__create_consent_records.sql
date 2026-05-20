
CREATE TABLE IF NOT EXISTS consent_records (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    mother_id       UUID        NOT NULL,
    consent_type    VARCHAR(32) NOT NULL,
    granted         BOOLEAN     NOT NULL,
    granted_by_role VARCHAR(32),
    consented_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMP,
    legal_basis     VARCHAR(64),
    revoked_at      TIMESTAMP,

    --  Constraints
  CONSTRAINT chk_consent_type CHECK (
    consent_type IN (
      'GOV_DATA_SHARE',
      'SMS_REMINDERS',
      'RESEARCH',
      'FACILITY_TRANSFER'
          )
      ),

  CONSTRAINT chk_consent_granted_role CHECK (
     granted_by_role IS NULL OR
     granted_by_role IN (
         'PATIENT',
         'HEALTH_WORKER',
         'FACILITY_ADMIN',
         'DISTRICT_OFFICER',
         'GOVERNMENT_ANALYST',
         'MOH_ADMIN'
         )
     ),

    -- A revoked consent must have been granted first
  CONSTRAINT chk_consent_revoke_after_grant CHECK (
     revoked_at IS NULL OR revoked_at >= consented_at
                                                   ),

    -- Expiry must be in the future relative to consent date
  CONSTRAINT chk_consent_expiry_after_grant CHECK (
     expires_at IS NULL OR expires_at > consented_at
                                                   ),

  CONSTRAINT fk_consent_mother
    FOREIGN KEY (mother_id)
      REFERENCES mothers (id)
      ON DELETE RESTRICT
      ON UPDATE CASCADE
);

--  Indexes
CREATE INDEX IF NOT EXISTS idx_consent_mother
    ON consent_records (mother_id);

CREATE INDEX IF NOT EXISTS idx_consent_type
    ON consent_records (mother_id, consent_type);

CREATE INDEX IF NOT EXISTS idx_consent_expiry
    ON consent_records (expires_at)
    WHERE expires_at IS NOT NULL;

-- Partial index for the HMIS push gate check:
-- "does this mother have an active, non-revoked GOV_DATA_SHARE consent?"
CREATE INDEX IF NOT EXISTS idx_consent_gov_active
    ON consent_records (mother_id)
    WHERE consent_type = 'GOV_DATA_SHARE'
        AND granted = TRUE
        AND revoked_at IS NULL;

-- View: active consents (helper for application layer)
CREATE OR REPLACE VIEW v_active_consents AS
SELECT
    mother_id,
    consent_type,
    granted_by_role,
    consented_at,
    expires_at,
    legal_basis
FROM consent_records
WHERE granted     = TRUE
  AND revoked_at  IS NULL
  AND (expires_at IS NULL OR expires_at > NOW());

-- Comments
COMMENT ON TABLE consent_records IS
    'REQUIRED before any mother data is shared with government '
        'systems. The application checks for an active GOV_DATA_SHARE '
        'consent before every HMIS push or DISTRICT_OFFICER query. '
        'Legal basis logged per Rwanda Law No. 058/2021.';

COMMENT ON VIEW v_active_consents IS
    'Convenience view: returns only consents that are granted, '
        'not revoked, and not expired. Use in @PreAuthorize checks '
        'before any government data exposure.';
