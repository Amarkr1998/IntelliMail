package com.intellimail.mail.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryExecutorTest {

    @Test
    void execute_returnsImmediately_whenFirstAttemptSucceeds() {
        AtomicInteger calls = new AtomicInteger();

        String result = RetryExecutor.execute(() -> {
            calls.incrementAndGet();
            return "ok";
        }, 3, 0, "test-op");

        assertThat(result).isEqualTo("ok");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void execute_retriesUntilSuccess_withinMaxAttempts() {
        AtomicInteger calls = new AtomicInteger();

        String result = RetryExecutor.execute(() -> {
            int attempt = calls.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("transient failure #" + attempt);
            }
            return "recovered";
        }, 5, 0, "test-op");

        assertThat(result).isEqualTo("recovered");
        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void execute_exhaustsAttempts_andRethrowsLastFailure() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> RetryExecutor.execute(() -> {
            calls.incrementAndGet();
            throw new RuntimeException("always fails");
        }, 3, 0, "test-op"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("always fails");

        assertThat(calls.get()).isEqualTo(3);
    }

    @Test
    void execute_treatsNonPositiveMaxAttempts_asOneAttempt() {
        AtomicInteger calls = new AtomicInteger();

        assertThatThrownBy(() -> RetryExecutor.execute(() -> {
            calls.incrementAndGet();
            throw new RuntimeException("fails once");
        }, 0, 0, "test-op"))
                .isInstanceOf(RuntimeException.class);

        assertThat(calls.get()).isEqualTo(1);
    }
}
