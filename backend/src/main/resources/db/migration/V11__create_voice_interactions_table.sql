-- Voice AI: a speech-to-text transcript (captured client-side via the Web Speech
-- API) and the AI-generated response to it. Independent of email_requests since a
-- voice prompt isn't necessarily about one specific existing email.
CREATE TABLE voice_interactions (
    id                UUID PRIMARY KEY,
    user_id           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    transcript        TEXT         NOT NULL,
    ai_response       TEXT,
    language          VARCHAR(40),
    ai_model          VARCHAR(60),
    prompt_tokens     INT,
    completion_tokens INT,
    total_tokens      INT,
    latency_ms        BIGINT,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_voice_interactions_user_id ON voice_interactions (user_id);
CREATE INDEX idx_voice_interactions_user_created ON voice_interactions (user_id, created_at DESC);
