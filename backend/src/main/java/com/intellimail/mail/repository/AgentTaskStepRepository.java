package com.intellimail.mail.repository;

import com.intellimail.mail.entity.AgentTaskStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AgentTaskStepRepository extends JpaRepository<AgentTaskStep, UUID> {

    List<AgentTaskStep> findByAgentTaskIdOrderByStepNumberAsc(UUID agentTaskId);
}
