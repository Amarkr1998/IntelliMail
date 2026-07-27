-- Password reset: an opaque, single-use token whose SHA-256 hash (never the raw
-- token itself) is stored, mirroring how user passwords are hashed - looked up
-- deterministically by hash rather than verified with a slow salted compare,
-- since the point here is a fast exact-match lookup, not password-grade
-- verification. Issued by POST /api/auth/forgot-password, consumed by
-- POST /api/auth/reset-password.
CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash  VARCHAR(64)  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    used_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_password_reset_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens (user_id);
