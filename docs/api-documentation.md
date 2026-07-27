# API Documentation

Base URL (dev): `http://localhost:8080`

Interactive documentation: **Swagger UI** at `/swagger-ui.html` (raw OpenAPI JSON at `/api-docs`) once the backend is running — every endpoint below has an `@Operation` summary/description there. A ready-to-run **Postman collection** is at [`postman/IntelliMail.postman_collection.json`](../postman/IntelliMail.postman_collection.json).

## Response Envelope

Every endpoint returns the same JSON shape:

```json
{
  "success": true,
  "message": "Success",
  "data": { "...": "..." },
  "timestamp": "2026-07-25T10:00:00Z"
}
```

On error, `success` is `false`, `data` is usually `null` (or a field-error map for validation failures), and `message` describes the problem. See the status-code table at the end of this document.

## Authentication

All endpoints except `/api/auth/**` and the Swagger/health endpoints require an `Authorization: Bearer <accessToken>` header. Access tokens expire after 15 minutes; use the refresh endpoint to get a new pair without re-authenticating.

| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Create an account (default role `ROLE_USER`) | No |
| POST | `/api/auth/login` | Exchange email/password for an access + refresh token pair | No |
| POST | `/api/auth/refresh` | Exchange a valid refresh token for a new pair | No |

**Register/Login response:**
```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresInMs": 900000,
  "user": { "id": "...", "fullName": "...", "email": "...", "roles": ["ROLE_USER"], "createdAt": "..." }
}
```

## User

| Method | Path | Description |
|---|---|---|
| GET | `/api/users/profile` | Get the current user's profile |
| PUT | `/api/users/profile` | Update `fullName` |

## Email (AI Generation)

All 20 core AI features are exposed through these 7 endpoints. `RequestType` is the shared enum selecting which system prompt is used internally.

| Method | Path | Request Body | Covers |
|---|---|---|---|
| POST | `/api/email/generate` | `{ originalContent, instructions?, promptTemplateId?, referenceContext? }` | AI Email Reply Generation |
| POST | `/api/email/improve` | `{ content, style, referenceContext? }` | Professional/Friendly/Formal/Casual Rewrite, Grammar Correction, Expand, Shorten |
| POST | `/api/email/translate` | `{ content, targetLanguage, referenceContext? }` | Email Translation |
| POST | `/api/email/summarize` | `{ content, referenceContext? }` | Email Summarization |
| POST | `/api/email/subject` | `{ content, referenceContext? }` | Subject Line Generator |
| POST | `/api/email/followup` | `{ originalContent, instructions?, referenceContext? }` | Follow-up Email Generator |
| POST | `/api/email/custom` | `{ requestType, context, customPrompt?, promptTemplateId?, referenceContext? }` | Meeting Request, Thank You, Apology, Sales, HR, Marketing, Cold Outreach, Custom AI Prompt |
| POST | `/api/email/extract` | `multipart/form-data`, field `file` | File Upload — extracts text from an uploaded file to use as `referenceContext` on any of the above |

`style` (for `/improve`) must be one of: `PROFESSIONAL_REWRITE`, `FRIENDLY_REWRITE`, `FORMAL_REWRITE`, `CASUAL_REWRITE`, `GRAMMAR_CORRECTION`, `EXPAND`, `SHORTEN`.

`requestType` (for `/custom`) must be one of: `MEETING_REQUEST`, `THANK_YOU`, `APOLOGY`, `SALES`, `HR`, `MARKETING`, `COLD_OUTREACH`, `CUSTOM_PROMPT`.

`referenceContext` (optional, max 20,000 characters, on every endpoint above) is **background information the AI may draw on — never the content being acted on itself**. It's the natural home for text extracted via `/api/email/extract`: e.g. attach a product spec sheet or pricing document, and it informs the reply without being mistaken for the email being replied to. Persisted on the `EmailRequest` row, so Reply Regeneration reuses the same reference material as the original attempt.

**Response (`EmailReplyResponse`):**
```json
{
  "id": "...", "emailRequestId": "...", "requestType": "GENERATE_REPLY",
  "content": "...", "aiModel": "gpt-4o", "attemptNumber": 1,
  "totalTokens": 87, "latencyMs": 812, "favorite": false, "createdAt": "..."
}
```

### File Upload (`/api/email/extract`)

Accepts PDF, Word (`.doc`/`.docx`), plain text, RTF, HTML, and most other common text-bearing formats (detected automatically via Apache Tika — the `accept` hint in the upload picker is just a suggestion, not an enforced allowlist). Max file size **10 MB** (`UPLOAD_MAX_FILE_SIZE`, see [`environment-variables.md`](environment-variables.md)). This endpoint never calls the AI and never persists anything itself — it only returns text, which the caller then submits as `referenceContext` (**not** `originalContent`/`content`) on any other `/api/email/*` endpoint.

**Request:** `multipart/form-data` with a single `file` field.

**Response (`FileExtractResponse`):**
```json
{
  "fileName": "meeting-notes.pdf",
  "content": "...",
  "characterCount": 1842,
  "truncated": false
}
```

If the extracted text exceeds 20,000 characters (the same cap as `originalContent`/`content` on every other `/api/email/*` request), it's truncated and `truncated` is `true` rather than the request failing.

## Voice AI

Speak a prompt instead of typing it: the browser transcribes speech to text client-side (via the Web Speech API), the transcript is sent here, and the AI's response is generated and persisted alongside it.

| Method | Path | Request Body | Description |
|---|---|---|---|
| POST | `/api/voice/prompt` | `{ transcript, language? }` | Submits a transcribed voice prompt and returns/persists the AI's response |
| GET | `/api/voice/history?page=&size=` | — | Paged list of past voice prompts and responses, newest first |

`transcript` (required, max 5,000 characters) is the speech-to-text result captured client-side. `language` (optional, max 40 characters) is a human-readable label (e.g. `"English (US)"`, `"Spanish"`) matching whatever language the browser was recognizing speech in — it steers the AI's reply into that language but plays no role in transcription itself, which already happened in the browser before this endpoint is called.

**Response (`VoiceResponse`):**
```json
{
  "id": "...",
  "transcript": "Reply saying Tuesday at 3pm works for me",
  "aiResponse": "Sure, Tuesday at 3pm works for me. Looking forward to it!",
  "language": "English (US)",
  "aiModel": "gpt-4o",
  "totalTokens": 58,
  "latencyMs": 640,
  "createdAt": "..."
}
```

Unlike the `/api/email/*` endpoints, a voice prompt is a single self-contained turn — there's no reply-attempt history or regeneration; each submission creates exactly one new row.

## History (includes Reply Regeneration & Favorite Replies)

| Method | Path | Description |
|---|---|---|
| GET | `/api/history?page=&size=` | Paged list of past requests, each with every reply attempt |
| DELETE | `/api/history/{id}` | Delete a request (cascades to its replies) |
| POST | `/api/history/{id}/regenerate` | Re-run the AI call for an existing request; adds a new attempt |
| PATCH | `/api/history/replies/{replyId}/favorite?favorite=true\|false` | Favorite/unfavorite a reply |

## Templates

| Method | Path | Description |
|---|---|---|
| GET | `/api/templates?page=&size=` | List the caller's templates + every public template |
| POST | `/api/templates` | Create a template |
| PUT | `/api/templates/{id}` | Update a template (owner only) |
| DELETE | `/api/templates/{id}` | Delete a template (owner only) |

**Request/response body (`PromptTemplateRequest`/`Response`):**
```json
{
  "name": "Friendly Cold Outreach", "description": "...", "category": "COLD_OUTREACH",
  "promptText": "Write a friendly, low-pressure cold outreach email about {{context}}",
  "systemPrompt": null, "isPublic": false
}
```

## Analytics

| Method | Path | Description |
|---|---|---|
| GET | `/api/analytics?from=&to=` | Per-`RequestType` breakdown of request count, tokens, avg latency. Defaults to the last 30 days if `from`/`to` are omitted (ISO-8601 date-times). |

## Status Codes

| Code | Meaning |
|---|---|
| 200 / 201 | Success |
| 400 | Validation failure — `data` is a `{ field: message }` map |
| 401 | Missing/invalid/expired token, or wrong email/password |
| 403 | Authenticated but not authorized (e.g. editing someone else's template) |
| 404 | Resource not found, **or** exists but not visible to you (private template/history entry you don't own) |
| 409 | Conflict (e.g. email already registered) |
| 502 | The Azure OpenAI call failed after all retries |
| 500 | Unexpected server error (message is intentionally generic; details are server-side logs only) |
