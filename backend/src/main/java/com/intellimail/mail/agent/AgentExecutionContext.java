package com.intellimail.mail.agent;

import java.util.UUID;

/**
 * Holds the current user/organization/task identity for the duration of one
 * {@code AgentOrchestrator} invocation, so agent tools can scope their work
 * to "the calling user" without the model ever supplying (or being trusted
 * with) a user id itself. Safe as a plain ThreadLocal because Spring AI's
 * default tool-calling manager invokes {@code @Tool} methods synchronously
 * on the same thread that made the {@code ChatClient} call.
 */
public final class AgentExecutionContext {

    public record Context(UUID userId, UUID organizationId, UUID taskId, String referenceContext) {
    }

    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private AgentExecutionContext() {
    }

    public static void set(UUID userId, UUID organizationId, UUID taskId, String referenceContext) {
        CURRENT.set(new Context(userId, organizationId, taskId, referenceContext));
    }

    public static Context current() {
        Context ctx = CURRENT.get();
        if (ctx == null) {
            throw new IllegalStateException("AgentExecutionContext accessed outside an agent task execution");
        }
        return ctx;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
