package com.intellimail.mail.agent;

import com.intellimail.mail.enums.PendingActionType;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgentPendingActionHolderTest {

    private final AgentPendingActionHolder holder = new AgentPendingActionHolder();

    @Test
    void drain_returnsProposedAction_andClearsIt() {
        holder.propose(PendingActionType.SAVE_TEMPLATE, Map.of("name", "Test"));

        Optional<AgentPendingActionHolder.PendingAction> first = holder.drain();

        assertThat(first).isPresent();
        assertThat(first.get().type()).isEqualTo(PendingActionType.SAVE_TEMPLATE);
        assertThat(holder.drain()).isEmpty();
    }

    @Test
    void drain_withNothingProposed_returnsEmpty() {
        assertThat(holder.drain()).isEmpty();
    }
}
