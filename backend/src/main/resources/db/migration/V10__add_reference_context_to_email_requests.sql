-- Reference material (e.g. text extracted from an uploaded file) that informs an AI
-- action as background context, without being confused with the actual email being
-- replied to/rewritten/translated. Persisted (not just used transiently) so Reply
-- Regeneration reuses the same reference material as the original attempt.
ALTER TABLE email_requests ADD COLUMN reference_context TEXT;
