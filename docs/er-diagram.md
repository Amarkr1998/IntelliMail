# Entity-Relationship Diagram

All primary keys are UUIDs (`GenerationType.UUID`), and every table has `created_at`/`updated_at` audit columns via Spring Data JPA auditing (omitted below for readability).

```mermaid
erDiagram
    ROLES ||--o{ USER_ROLES : "assigned via"
    USERS ||--o{ USER_ROLES : "has"
    USERS ||--o{ PROMPT_TEMPLATES : owns
    USERS ||--o{ EMAIL_REQUESTS : submits
    USERS ||--o{ FEEDBACK : gives
    USERS ||--o{ USAGE_ANALYTICS : generates
    USERS ||--o{ AUDIT_LOGS : "acts (nullable)"
    PROMPT_TEMPLATES |o--o{ EMAIL_REQUESTS : "optionally used by"
    EMAIL_REQUESTS ||--o{ GENERATED_REPLIES : produces
    GENERATED_REPLIES ||--o{ FEEDBACK : receives

    ROLES {
        uuid id PK
        string name "ROLE_USER / ROLE_ADMIN"
        string description
    }

    USERS {
        uuid id PK
        string full_name
        string email UK
        string password "BCrypt hash"
        boolean enabled
    }

    USER_ROLES {
        uuid user_id FK
        uuid role_id FK
    }

    PROMPT_TEMPLATES {
        uuid id PK
        string name
        string description
        string category "RequestType"
        text prompt_text
        text system_prompt "nullable override"
        uuid owner_id FK "nullable = system template"
        boolean is_public
    }

    EMAIL_REQUESTS {
        uuid id PK
        uuid user_id FK
        string request_type "RequestType enum"
        text original_content
        text instructions
        string target_language
        text reference_context "background info only, e.g. from an uploaded file"
        uuid prompt_template_id FK "nullable"
    }

    GENERATED_REPLIES {
        uuid id PK
        uuid email_request_id FK
        text content
        string ai_model
        int attempt_number
        int prompt_tokens
        int completion_tokens
        int total_tokens
        bigint latency_ms
        boolean is_favorite
    }

    FEEDBACK {
        uuid id PK
        uuid generated_reply_id FK
        uuid user_id FK
        int rating "1-5"
        text comment
    }

    USAGE_ANALYTICS {
        uuid id PK
        uuid user_id FK
        string request_type
        int tokens_used
        bigint latency_ms
        boolean success
        string error_message
    }

    AUDIT_LOGS {
        uuid id PK
        uuid user_id FK "nullable"
        string action
        string entity_type
        string entity_id
        text details
        string ip_address
    }
```

## Notable Design Decisions

- **`GeneratedReply` is many-to-one with `EmailRequest`, not one-to-one.** Reply Regeneration inserts a new row with an incremented `attempt_number` rather than overwriting the previous reply — full history is preserved, and `GET /api/history` returns every attempt.
- **`PromptTemplate.owner_id` is nullable.** A `null` owner (reserved for a future system/seed user) combined with `is_public = true` marks a template visible to everyone; a template with an owner and `is_public = false` is private to that user only.
- **`UsageAnalytics` is independent of `EmailRequest`**, not derived from it. It's written even when the AI call fails (with `success = false` and an `error_message`), and it survives if the originating `EmailRequest` is later deleted via `DELETE /api/history/{id}` (no FK from `UsageAnalytics` back to `EmailRequest`).
- **`AuditLog.user_id` is nullable** to support logging unauthenticated/system events (e.g. a failed login attempt against a real user id, or a future system-initiated action).
- **No `Feedback` REST endpoint currently exists** (see [`future-enhancements.md`](future-enhancements.md)) — the table and repository are in place, ready for a `POST /api/history/replies/{id}/feedback` endpoint.
