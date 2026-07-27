-- Set once a user links or auto-registers via Google Sign-In; NULL for every
-- pre-existing account and every account that never uses it. Postgres unique
-- constraints allow multiple NULLs, so no partial-index workaround is needed.
ALTER TABLE users
    ADD COLUMN google_subject_id VARCHAR(255),
    ADD CONSTRAINT uk_users_google_subject_id UNIQUE (google_subject_id);
