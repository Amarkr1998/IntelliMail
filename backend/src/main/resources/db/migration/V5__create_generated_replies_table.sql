CREATE TABLE generated_replies (
    id                 UUID PRIMARY KEY,
    email_request_id   UUID        NOT NULL REFERENCES email_requests (id) ON DELETE CASCADE,
    content            TEXT        NOT NULL,
    ai_model           VARCHAR(60),
    attempt_number     INT         NOT NULL DEFAULT 1,
    prompt_tokens      INT,
    completion_tokens  INT,
    total_tokens       INT,
    latency_ms         BIGINT,
    is_favorite        BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_generated_replies_email_request_id ON generated_replies (email_request_id);
