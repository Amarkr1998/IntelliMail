package com.intellimail.mail.entity;

import com.intellimail.mail.enums.RequestType;
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
 * Captures a single AI request submitted by a user (e.g. from the Chrome
 * extension or the Compose Assistant page) before the AI response is
 * generated. One {@code EmailRequest} may have several {@link GeneratedReply}
 * rows when the user regenerates a reply.
 */
@Entity
@Table(name = "email_requests")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class EmailRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 40)
    private RequestType requestType;

    @Column(name = "original_content", nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "target_language", length = 40)
    private String targetLanguage;

    // Matches migration V4's ON DELETE SET NULL exactly, so Hibernate's auto-generated
    // test schema (H2, ddl-auto=create-drop) behaves the same as the Flyway-managed
    // production schema instead of falling back to the default RESTRICT/NO ACTION -
    // without this, deleting a PromptTemplate still referenced by an EmailRequest only
    // fails in tests, not in production, which is exactly backwards for catching bugs.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private PromptTemplate promptTemplate;
}
