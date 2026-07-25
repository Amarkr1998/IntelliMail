package com.intellimail.mail.util;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

/**
 * Minimal, framework-agnostic retry helper with linear backoff, used to wrap
 * flaky outbound calls (the Azure OpenAI client, in particular). Kept free of
 * any AI/HTTP dependency so retry behavior itself can be unit tested without
 * mocking a third-party client.
 */
@Slf4j
public final class RetryExecutor {

    private RetryExecutor() {
    }

    /**
     * Invokes {@code action} up to {@code maxAttempts} times, sleeping
     * {@code backoffMs * attempt} between failures. Rethrows the last
     * failure if every attempt fails.
     */
    public static <T> T execute(Supplier<T> action, int maxAttempts, long backoffMs, String operationName) {
        int attempts = Math.max(1, maxAttempts);
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException ex) {
                lastFailure = ex;
                log.warn("{} failed (attempt {}/{}): {}", operationName, attempt, attempts, ex.getMessage());
                if (attempt < attempts) {
                    sleep(backoffMs * attempt);
                }
            }
        }
        throw lastFailure;
    }

    private static void sleep(long millis) {
        if (millis <= 0) {
            return;
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry", ie);
        }
    }
}
