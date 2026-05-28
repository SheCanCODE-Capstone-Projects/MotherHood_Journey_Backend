-- gov_sync_log.reference_no: UUID strings are 36 chars, column was VARCHAR(32)
ALTER TABLE gov_sync_log
    ALTER COLUMN reference_no TYPE VARCHAR(64);

-- consent_records.legal_basis: free-text legal basis exceeds 32 chars
ALTER TABLE consent_records
    ALTER COLUMN legal_basis TYPE VARCHAR(255);
