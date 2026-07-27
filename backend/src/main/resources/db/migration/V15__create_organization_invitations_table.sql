-- Mirrors password_reset_tokens' hashed-single-use-token pattern. invited_by_id
-- is ON DELETE SET NULL (not CASCADE) so a pending invitation survives its
-- inviter later leaving the organization.
CREATE TABLE organization_invitations (
    id              UUID          PRIMARY KEY,
    organization_id UUID          NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    email           VARCHAR(255)  NOT NULL,
    org_role        VARCHAR(20)   NOT NULL,
    invited_by_id   UUID          REFERENCES users (id) ON DELETE SET NULL,
    token_hash      VARCHAR(64)   NOT NULL,
    expires_at      TIMESTAMPTZ   NOT NULL,
    accepted_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL,
    updated_at      TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_organization_invitations_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_organization_invitations_organization_id ON organization_invitations (organization_id);
CREATE INDEX idx_organization_invitations_email ON organization_invitations (email);
