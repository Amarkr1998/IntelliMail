package com.intellimail.mail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * An AI-generated response to an {@link EmailRequest}. Regenerating a reply
 * creates a new row with an incremented {@code attemptNumber} rather than
 * overwriting the previous one, preserving full history.
 */
@Entity
@Table(name = "generated_replies")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class GeneratedReply extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "email_request_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private EmailRequest emailRequest;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "ai_model", length = 60)
    private String aiModel;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private int attemptNumber = 1;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "is_favorite", nullable = false)
    @Builder.Default
    private boolean favorite = false;
}
