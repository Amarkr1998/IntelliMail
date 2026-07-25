CREATE TABLE email_requests (
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    request_type      VARCHAR(40)  NOT NULL,
    original_content  TEXT         NOT NULL,
    instructions      TEXT,
    target_language   VARCHAR(40),
    prompt_template_id UUID REFERENCES prompt_templates (id) ON DELETE SET NULL,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_email_requests_user_id ON email_requests (user_id);
CREATE INDEX idx_email_requests_user_created ON email_requests (user_id, created_at DESC);
