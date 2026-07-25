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
| POST | `/api/email/generate` | `{ originalContent, instructions?, promptTemplateId? }` | AI Email Reply Generation |
| POST | `/api/email/improve` | `{ content, style }` | Professional/Friendly/Formal/Casual Rewrite, Grammar Correction, Expand, Shorten |
| POST | `/api/email/translate` | `{ content, targetLanguage }` | Email Translation |
| POST | `/api/email/summarize` | `{ content }` | Email Summarization |
| POST | `/api/email/subject` | `{ content }` | Subject Line Generator |
| POST | `/api/email/followup` | `{ originalContent, instructions? }` | Follow-up Email Generator |
| POST | `/api/email/custom` | `{ requestType, context, customPrompt?, promptTemplateId? }` | Meeting Request, Thank You, Apology, Sales, HR, Marketing, Cold Outreach, Custom AI Prompt |

`style` (for `/improve`) must be one of: `PROFESSIONAL_REWRITE`, `FRIENDLY_REWRITE`, `FORMAL_REWRITE`, `CASUAL_REWRITE`, `GRAMMAR_CORRECTION`, `EXPAND`, `SHORTEN`.

`requestType` (for `/custom`) must be one of: `MEETING_REQUEST`, `THANK_YOU`, `APOLOGY`, `SALES`, `HR`, `MARKETING`, `COLD_OUTREACH`, `CUSTOM_PROMPT`.

**Response (`EmailReplyResponse`):**
```json
{
  "id": "...", "emailRequestId": "...", "requestType": "GENERATE_REPLY",
  "content": "...", "aiModel": "gpt-4o", "attemptNumber": 1,
  "totalTokens": 87, "latencyMs": 812, "favorite": false, "createdAt": "..."
}
```

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
