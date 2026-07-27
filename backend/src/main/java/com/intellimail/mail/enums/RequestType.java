package com.intellimail.mail.enums;

/**
 * Every distinct AI email-automation capability offered by the platform.
 * Drives prompt selection in the {@code prompt} package and is recorded on
 * {@code EmailRequest} / {@code UsageAnalytics} for history and reporting.
 */
public enum RequestType {
    GENERATE_REPLY,
    PROFESSIONAL_REWRITE,
    FRIENDLY_REWRITE,
    FORMAL_REWRITE,
    CASUAL_REWRITE,
    GRAMMAR_CORRECTION,
    SUMMARIZE,
    TRANSLATE,
    SUBJECT_LINE,
    EXPAND,
    SHORTEN,
    FOLLOWUP,
    MEETING_REQUEST,
    THANK_YOU,
    APOLOGY,
    SALES,
    HR,
    MARKETING,
    COLD_OUTREACH,
    CUSTOM_PROMPT,
    VOICE_COMMAND
}
