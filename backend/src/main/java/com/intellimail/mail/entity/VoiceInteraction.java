package com.intellimail.mail.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * A single Voice AI turn: a speech-to-text transcript submitted by the user
 * (via the Web Speech API in the browser) and the AI-generated response to
 * it. Persisted independently of {@link EmailRequest} since a voice prompt
 * isn't necessarily about one specific existing email.
 */
@Entity
@Table(name = "voice_interactions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VoiceInteraction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "transcript", nullable = false, columnDefinition = "TEXT")
    private String transcript;

    @Column(name = "ai_response", columnDefinition = "TEXT")
    private String aiResponse;

    /** BCP-47-ish language label the user picked in the UI (e.g. "English (US)"), used to steer the AI's reply language. */
    @Column(name = "language", length = 40)
    private String language;

    @Column(name = "ai_model", length = 60)
    private String aiModel;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "latency_ms")
    private Long latencyMs;
}
