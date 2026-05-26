-- ============================================================
-- V13__Refresh_Tokens.sql
-- Persistent refresh tokens for JWT rotation + revocation.
-- Tokens are stored as SHA-256 hashes; raw values are never persisted.
-- ============================================================

CREATE TABLE refresh_tokens (
    id                       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                  UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash               VARCHAR(64)  NOT NULL UNIQUE,
    issued_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at               TIMESTAMP    NOT NULL,
    revoked_at               TIMESTAMP,
    replaced_by_token_hash   VARCHAR(64),
    ip_address               VARCHAR(45),
    user_agent               VARCHAR(255)
);

CREATE INDEX idx_refresh_user      ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_expires   ON refresh_tokens (expires_at);
