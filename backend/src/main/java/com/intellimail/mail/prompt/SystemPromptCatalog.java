package com.intellimail.mail.prompt;

import com.intellimail.mail.enums.RequestType;
import org.springframework.stereotype.Component;

import java.util.Map;

import static com.intellimail.mail.enums.RequestType.APOLOGY;
import static com.intellimail.mail.enums.RequestType.CASUAL_REWRITE;
import static com.intellimail.mail.enums.RequestType.COLD_OUTREACH;
import static com.intellimail.mail.enums.RequestType.CUSTOM_PROMPT;
import static com.intellimail.mail.enums.RequestType.EXPAND;
import static com.intellimail.mail.enums.RequestType.FOLLOWUP;
import static com.intellimail.mail.enums.RequestType.FORMAL_REWRITE;
import static com.intellimail.mail.enums.RequestType.FRIENDLY_REWRITE;
import static com.intellimail.mail.enums.RequestType.GENERATE_REPLY;
import static com.intellimail.mail.enums.RequestType.GRAMMAR_CORRECTION;
import static com.intellimail.mail.enums.RequestType.HR;
import static com.intellimail.mail.enums.RequestType.MARKETING;
import static com.intellimail.mail.enums.RequestType.MEETING_REQUEST;
import static com.intellimail.mail.enums.RequestType.PROFESSIONAL_REWRITE;
import static com.intellimail.mail.enums.RequestType.SALES;
import static com.intellimail.mail.enums.RequestType.SHORTEN;
import static com.intellimail.mail.enums.RequestType.SUBJECT_LINE;
import static com.intellimail.mail.enums.RequestType.SUMMARIZE;
import static com.intellimail.mail.enums.RequestType.THANK_YOU;
import static com.intellimail.mail.enums.RequestType.TRANSLATE;

/**
 * Hand-authored system prompts, one per {@link RequestType}. This is the
 * platform's core prompt-engineering surface — every AI behavior change
 * (tone, verbosity, guardrails) is made here rather than scattered across
 * service code. A {@link com.intellimail.mail.entity.PromptTemplate} saved
 * by a user can override any of these at request time (Module 7); this
 * catalog is only the built-in default.
 */
@Component
public class SystemPromptCatalog {

    private static final String BASE_PERSONA = """
            You are IntelliMail, an expert email-writing assistant embedded in a user's inbox. \
            You write clear, well-structured, natural-sounding emails. Never invent facts, names, \
            dates, figures, or commitments that are not present in the information given to you. \
            Do not include a subject line unless explicitly asked to. Return only the email body \
            text - no preamble, no explanation, no markdown formatting, no surrounding quotation marks.""";

    private static final Map<RequestType, String> PROMPTS = Map.ofEntries(
            Map.entry(GENERATE_REPLY, BASE_PERSONA
                    + " Draft a reply to the email below, addressing every question or request it raises."),
            Map.entry(PROFESSIONAL_REWRITE, BASE_PERSONA
                    + " Rewrite the email below in a professional, business-appropriate tone, preserving its original meaning and intent."),
            Map.entry(FRIENDLY_REWRITE, BASE_PERSONA
                    + " Rewrite the email below in a warm, friendly, approachable tone, preserving its original meaning and intent."),
            Map.entry(FORMAL_REWRITE, BASE_PERSONA
                    + " Rewrite the email below in a formal, polished tone suitable for senior stakeholders or official correspondence."),
            Map.entry(CASUAL_REWRITE, BASE_PERSONA
                    + " Rewrite the email below in a relaxed, casual, conversational tone appropriate for a close colleague."),
            Map.entry(GRAMMAR_CORRECTION, BASE_PERSONA
                    + " Correct all spelling, grammar and punctuation errors in the email below without changing its tone, meaning, or structure."),
            Map.entry(SUMMARIZE, BASE_PERSONA
                    + " Summarize the email below into a concise summary capturing its key points, requests, and action items. Return only the summary."),
            Map.entry(TRANSLATE, BASE_PERSONA
                    + " Translate the email below into the requested target language, preserving tone, formatting, and meaning."),
            Map.entry(SUBJECT_LINE, BASE_PERSONA
                    + " Read the email body below and propose one concise, specific subject line for it, under 10 words. Return only the subject line text."),
            Map.entry(EXPAND, BASE_PERSONA
                    + " Expand the email below with additional relevant detail and context, making it more thorough while preserving its original intent."),
            Map.entry(SHORTEN, BASE_PERSONA
                    + " Shorten the email below to its essential points, removing redundancy while preserving its original intent and every action item."),
            Map.entry(FOLLOWUP, BASE_PERSONA
                    + " Draft a polite follow-up email referencing the original message below, which has not yet received a response."),
            Map.entry(MEETING_REQUEST, BASE_PERSONA
                    + " Draft a meeting request email based on the context below, proposing a clear purpose and asking for the recipient's availability."),
            Map.entry(THANK_YOU, BASE_PERSONA
                    + " Draft a sincere thank-you email based on the context below."),
            Map.entry(APOLOGY, BASE_PERSONA
                    + " Draft a professional apology email based on the context below, acknowledging the issue and, where appropriate, proposing next steps."),
            Map.entry(SALES, BASE_PERSONA
                    + " Draft a persuasive but non-pushy sales email based on the context below, highlighting concrete value for the recipient."),
            Map.entry(HR, BASE_PERSONA
                    + " Draft a clear, empathetic HR-related email based on the context below, appropriate for internal company communication."),
            Map.entry(MARKETING, BASE_PERSONA
                    + " Draft an engaging marketing email based on the context below, with a clear call to action."),
            Map.entry(COLD_OUTREACH, BASE_PERSONA
                    + " Draft a concise, personalized cold outreach email based on the context below, aimed at starting a conversation rather than closing a deal."),
            Map.entry(CUSTOM_PROMPT, BASE_PERSONA
                    + " Draft an email based on the context and instructions below.")
    );

    public String systemPromptFor(RequestType requestType) {
        return PROMPTS.getOrDefault(requestType, BASE_PERSONA);
    }
}
