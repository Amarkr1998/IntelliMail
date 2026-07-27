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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

/**
 * A reusable, user- or system-authored prompt used to steer AI generation for
 * a given {@link RequestType}. Templates with a {@code null} owner are global
 * system templates visible to every user.
 */
@Entity
@Table(name = "prompt_templates")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PromptTemplate extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private RequestType category;

    @Column(name = "prompt_text", nullable = false, columnDefinition = "TEXT")
    private String promptText;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User owner;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private boolean isPublic = false;

    /**
     * Bare column, not a mapped relationship - only ever used to scope the
     * {@code isPublic} visibility predicate (see
     * {@link com.intellimail.mail.repository.PromptTemplateRepository#findVisibleToUser}),
     * never navigated, so it avoids an extra join/lazy-load on the
     * template-list hot path. Null for solo users and every template created
     * before organizations existed - their visibility is unchanged.
     */
    @Column(name = "organization_id")
    private UUID organizationId;
}
