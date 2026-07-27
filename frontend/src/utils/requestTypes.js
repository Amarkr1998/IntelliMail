// Mirrors com.intellimail.mail.enums.RequestType on the backend.

export const REWRITE_STYLES = [
  { value: 'PROFESSIONAL_REWRITE', label: 'Professional' },
  { value: 'FRIENDLY_REWRITE', label: 'Friendly' },
  { value: 'FORMAL_REWRITE', label: 'Formal' },
  { value: 'CASUAL_REWRITE', label: 'Casual' },
  { value: 'GRAMMAR_CORRECTION', label: 'Grammar Correction' },
  { value: 'EXPAND', label: 'Expand' },
  { value: 'SHORTEN', label: 'Shorten' },
];

export const CUSTOM_GENERATOR_TYPES = [
  { value: 'MEETING_REQUEST', label: 'Meeting Request' },
  { value: 'THANK_YOU', label: 'Thank You' },
  { value: 'APOLOGY', label: 'Apology' },
  { value: 'SALES', label: 'Sales' },
  { value: 'HR', label: 'HR' },
  { value: 'MARKETING', label: 'Marketing' },
  { value: 'COLD_OUTREACH', label: 'Cold Outreach' },
  { value: 'CUSTOM_PROMPT', label: 'Custom Prompt' },
];

export const REQUEST_TYPE_LABELS = {
  GENERATE_REPLY: 'Reply Generation',
  PROFESSIONAL_REWRITE: 'Professional Rewrite',
  FRIENDLY_REWRITE: 'Friendly Rewrite',
  FORMAL_REWRITE: 'Formal Rewrite',
  CASUAL_REWRITE: 'Casual Rewrite',
  GRAMMAR_CORRECTION: 'Grammar Correction',
  SUMMARIZE: 'Summarize',
  TRANSLATE: 'Translate',
  SUBJECT_LINE: 'Subject Line',
  EXPAND: 'Expand',
  SHORTEN: 'Shorten',
  FOLLOWUP: 'Follow-up',
  MEETING_REQUEST: 'Meeting Request',
  THANK_YOU: 'Thank You',
  APOLOGY: 'Apology',
  SALES: 'Sales',
  HR: 'HR',
  MARKETING: 'Marketing',
  COLD_OUTREACH: 'Cold Outreach',
  CUSTOM_PROMPT: 'Custom Prompt',
  VOICE_COMMAND: 'Voice AI',
};
