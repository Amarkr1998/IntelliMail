package com.intellimail.mail.entity;

import com.intellimail.mail.enums.AgentTaskStatus;
import com.intellimail.mail.enums.PendingActionType;
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

import java.util.UUID;

/**
 * One row per goal submitted to the AI agent orchestrator, whether starting
 * a new conversation or continuing one via {@link #conversationId}. The
 * {@code pendingAction*} columns are folded on directly rather than a
 * separate table since v1 only ever has one possible pending-action type
 * (saving a template) - see {@code agent.AgentPendingActionHolder}.
 */
@Entity
@Table(name = "agent_tasks")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AgentTask extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    // Bare (unmapped-as-a-relationship) column, matching PromptTemplate's
    // organizationId - never navigated, only used to scope task history.
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentTaskStatus status;

    @Column(name = "final_result", columnDefinition = "TEXT")
    private String finalResult;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_action_type", length = 30)
    private PendingActionType pendingActionType;

    /** JSON-serialized payload for {@link #pendingActionType}, applied verbatim on confirm. */
    @Column(name = "pending_action_payload", columnDefinition = "TEXT")
    private String pendingActionPayload;
}
