-- Both columns are nullable: organization membership is opt-in. A user with
-- organization_id = NULL is a solo user and org_role is meaningless for them.
ALTER TABLE users
    ADD COLUMN organization_id UUID REFERENCES organizations (id) ON DELETE SET NULL,
    ADD COLUMN org_role        VARCHAR(20),
    ADD CONSTRAINT ck_users_org_role_requires_organization
        CHECK ( (organization_id IS NULL) = (org_role IS NULL) );

CREATE INDEX idx_users_organization_id ON users (organization_id);
