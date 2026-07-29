-- One row per individual tool invocation the agent made in service of one
-- agent_task, in order - the trail the Task History UI renders as a
-- timeline. Flat/ordered only: no parallel or branching concepts, matching
-- the deliberately-scoped-down design of this feature.
CREATE TABLE agent_task_steps (
    id             UUID         PRIMARY KEY,
    agent_task_id  UUID         NOT NULL REFERENCES agent_tasks (id) ON DELETE CASCADE,
    step_number    INT          NOT NULL,
    tool_name      VARCHAR(100) NOT NULL,
    input_summary  TEXT,
    output_summary TEXT,
    status         VARCHAR(20)  NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_agent_task_steps_task_step UNIQUE (agent_task_id, step_number)
);

CREATE INDEX idx_agent_task_steps_task_id ON agent_task_steps (agent_task_id);
