CREATE TABLE prompt_templates (
    id             UUID PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    description    VARCHAR(500),
    category       VARCHAR(40)  NOT NULL,
    prompt_text    TEXT         NOT NULL,
    system_prompt  TEXT,
    owner_id       UUID REFERENCES users (id) ON DELETE CASCADE,
    is_public      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_prompt_templates_owner_id ON prompt_templates (owner_id);
CREATE INDEX idx_prompt_templates_category ON prompt_templates (category);
