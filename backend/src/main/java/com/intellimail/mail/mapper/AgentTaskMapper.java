package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.agent.AgentStepResponse;
import com.intellimail.mail.dto.agent.AgentTaskSummaryResponse;
import com.intellimail.mail.entity.AgentTask;
import com.intellimail.mail.entity.AgentTaskStep;
import org.mapstruct.Mapper;

/**
 * The full AgentTaskResponse (steps list + pendingAction) is assembled by
 * AgentOrchestrator instead of here, since the pending-action payload needs
 * JSON deserialization that doesn't belong in a MapStruct mapper.
 */
@Mapper(componentModel = "spring")
public interface AgentTaskMapper {

    AgentStepResponse toStepResponse(AgentTaskStep step);

    AgentTaskSummaryResponse toSummaryResponse(AgentTask task);
}
