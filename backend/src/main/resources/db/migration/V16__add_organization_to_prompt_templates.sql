-- Bare (unmapped-as-a-relationship) column: only ever used to scope the
-- isPublic visibility predicate, never navigated, so no extra join/lazy-load
-- on the template-list hot path. NULL for solo users and every pre-existing
-- template - their visibility behavior is unchanged by this migration.
ALTER TABLE prompt_templates
    ADD COLUMN organization_id UUID REFERENCES organizations (id) ON DELETE SET NULL;

CREATE INDEX idx_prompt_templates_organization_id ON prompt_templates (organization_id);
