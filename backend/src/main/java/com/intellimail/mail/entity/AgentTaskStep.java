package com.intellimail.mail.entity;

import com.intellimail.mail.enums.AgentStepStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * One row per tool the agent invoked while working on an {@link AgentTask},
 * in order - the trail the Task History UI renders as a timeline.
 */
@Entity
@Table(name = "agent_task_steps")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AgentTaskStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_task_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private AgentTask agentTask;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(name = "tool_name", nullable = false, length = 100)
    private String toolName;

    @Column(name = "input_summary", columnDefinition = "TEXT")
    private String inputSummary;

    @Column(name = "output_summary", columnDefinition = "TEXT")
    private String outputSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AgentStepStatus status;
}
