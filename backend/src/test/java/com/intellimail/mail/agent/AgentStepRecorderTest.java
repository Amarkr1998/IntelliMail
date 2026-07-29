package com.intellimail.mail.agent;

import com.intellimail.mail.enums.AgentStepStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentStepRecorderTest {

    private final AgentStepRecorder recorder = new AgentStepRecorder();

    @Test
    void drain_returnsRecordedStepsInOrder_andClearsThemAfterwards() {
        recorder.record("toolA", "in-a", "out-a", AgentStepStatus.SUCCESS);
        recorder.record("toolB", "in-b", "out-b", AgentStepStatus.FAILED);

        List<AgentStepRecorder.RecordedStep> steps = recorder.drain();

        assertThat(steps).hasSize(2);
        assertThat(steps.get(0).toolName()).isEqualTo("toolA");
        assertThat(steps.get(1).status()).isEqualTo(AgentStepStatus.FAILED);
        assertThat(recorder.drain()).isEmpty();
    }

    @Test
    void record_truncatesOverlongSummaries() {
        String longText = "x".repeat(1000);

        recorder.record("tool", longText, longText, AgentStepStatus.SUCCESS);

        AgentStepRecorder.RecordedStep step = recorder.drain().get(0);
        assertThat(step.inputSummary()).hasSize(503).endsWith("...");
    }
}
