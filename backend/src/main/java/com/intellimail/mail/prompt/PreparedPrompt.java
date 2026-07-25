package com.intellimail.mail.prompt;

/** A fully assembled system + user prompt pair, ready to hand to the Azure OpenAI client. */
public record PreparedPrompt(String systemPrompt, String userPrompt) {
}
