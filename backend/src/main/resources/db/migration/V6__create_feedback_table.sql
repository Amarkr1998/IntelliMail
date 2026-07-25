CREATE TABLE feedback (
    id                  UUID PRIMARY KEY,
    generated_reply_id  UUID        NOT NULL REFERENCES generated_replies (id) ON DELETE CASCADE,
    user_id             UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating              INT         NOT NULL,
    comment             TEXT,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_feedback_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_feedback_generated_reply_id ON feedback (generated_reply_id);
CREATE INDEX idx_feedback_user_id ON feedback (user_id);
