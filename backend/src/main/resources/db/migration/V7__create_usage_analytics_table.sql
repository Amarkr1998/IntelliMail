CREATE TABLE usage_analytics (
    id            UUID PRIMARY KEY,
    user_id       UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    request_type  VARCHAR(40) NOT NULL,
    tokens_used   INT,
    latency_ms    BIGINT,
    success       BOOLEAN     NOT NULL DEFAULT TRUE,
    error_message VARCHAR(500),
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_usage_analytics_user_id ON usage_analytics (user_id);
CREATE INDEX idx_usage_analytics_user_created ON usage_analytics (user_id, created_at);
