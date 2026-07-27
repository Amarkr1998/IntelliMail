package com.intellimail.mail.dto.voice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Powers POST /api/voice/prompt — a speech-to-text transcript captured client-side via the Web Speech API. */
public record VoicePromptRequest(

        @NotBlank(message = "Voice transcript is required")
        @Size(max = 5_000, message = "Voice transcript must not exceed 5,000 characters")
        String transcript,

        @Size(max = 40, message = "Language must not exceed 40 characters")
        String language
) {
    // language is an optional human-readable label (e.g. "English (US)", "Spanish")
    // taken from the same dropdown that sets the browser's SpeechRecognition.lang -
    // used to steer the AI's response language, not to control transcription itself.
}
