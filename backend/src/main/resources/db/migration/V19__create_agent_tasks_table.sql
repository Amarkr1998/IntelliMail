-- Agent Task: one persisted row per user goal submitted to the AI agent
-- orchestrator (POST /api/agent/tasks), whether starting a new conversation
-- or continuing one via conversation_id. Deliberately not a generic
-- workflow-engine table - status is a small fixed set, and the two
-- pending_action_* columns are folded on directly (rather than a separate
-- PendingAgentAction table) since v1 only ever has one possible
-- pending-action type (SAVE_TEMPLATE).
CREATE TABLE agent_tasks (
    id                     UUID         PRIMARY KEY,
    user_id                UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    organization_id        UUID,
    goal                   TEXT         NOT NULL,
    status                 VARCHAR(30)  NOT NULL,
    final_result           TEXT,
    conversation_id        UUID         NOT NULL,
    pending_action_type    VARCHAR(30),
    pending_action_payload TEXT,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_agent_tasks_user_id ON agent_tasks (user_id);
CREATE INDEX idx_agent_tasks_user_created ON agent_tasks (user_id, created_at DESC);
CREATE INDEX idx_agent_tasks_conversation_id ON agent_tasks (conversation_id);
