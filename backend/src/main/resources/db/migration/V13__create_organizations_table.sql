-- Organizations/workspaces for the opt-in multi-tenancy feature. A user with
-- no organization keeps behaving exactly as before this migration.
CREATE TABLE organizations (
    id          UUID          PRIMARY KEY,
    name        VARCHAR(150)  NOT NULL,
    slug        VARCHAR(80)   NOT NULL,
    logo_url    VARCHAR(500),
    brand_color VARCHAR(20),
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_organizations_slug UNIQUE (slug)
);

CREATE INDEX idx_organizations_slug ON organizations (slug);
