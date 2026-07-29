package com.intellimail.mail.agent;

import com.intellimail.mail.enums.PendingActionType;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

/**
 * Holds a single proposed-but-not-yet-executed mutating action for the
 * current agent task (v1 only ever has one possible type: SAVE_TEMPLATE).
 * The proposing tool never performs the mutation itself - see
 * {@code tools.SaveTemplateAgentTool} and {@code AgentOrchestrator}'s
 * confirm/reject endpoints for the human-in-the-loop gate this backs.
 */
@Component
public class AgentPendingActionHolder {

    public record PendingAction(PendingActionType type, Map<String, Object> payload) {
    }

    private static final ThreadLocal<PendingAction> CURRENT = new ThreadLocal<>();

    public void propose(PendingActionType type, Map<String, Object> payload) {
        CURRENT.set(new PendingAction(type, payload));
    }

    /** Returns the proposed action (if any) and clears it. */
    public Optional<PendingAction> drain() {
        PendingAction action = CURRENT.get();
        CURRENT.remove();
        return Optional.ofNullable(action);
    }
}
