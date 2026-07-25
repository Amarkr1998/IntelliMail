# Sequence Diagram — AI Reply Generation (Gmail → Extension → Backend → Azure OpenAI)

This is the flow described in the original application spec: a user reads an email in Gmail, clicks the extension's AI button, and the generated reply is inserted back into the compose box.

```mermaid
sequenceDiagram
    actor User
    participant Gmail
    participant Content as Content Script
    participant BG as Background Service Worker
    participant API as Spring Boot API
    participant Prompt as PromptFactory
    participant AOAI as Azure OpenAI
    participant DB as PostgreSQL

    User->>Gmail: Opens an email, clicks the floating "AI" button
    Content->>Content: Read open email body (div.a3s.aiL)
    Content->>BG: sendMessage({ type: "GENERATE_REPLY", payload })
    BG->>BG: Read JWT from chrome.storage.local
    BG->>API: POST /api/email/generate (Bearer token)

    alt Access token expired
        API-->>BG: 401 Unauthorized
        BG->>API: POST /api/auth/refresh
        API-->>BG: new access + refresh tokens
        BG->>API: Retry POST /api/email/generate
    end

    API->>API: JwtAuthenticationFilter validates token, sets SecurityContext
    API->>DB: Save EmailRequest (requestType=GENERATE_REPLY)
    API->>Prompt: forGenerateReply(content, instructions, templateOverride)
    Prompt-->>API: PreparedPrompt(system, user)
    API->>AOAI: ChatClient.prompt().system(...).user(...).call()

    alt AI call fails after retries
        AOAI-->>API: error
        API->>DB: Record UsageAnalytics(success=false) [own transaction]
        API-->>BG: 502 Bad Gateway
        BG-->>Content: { success:false, error }
        Content->>User: Show error status in panel
    else AI call succeeds
        AOAI-->>API: ChatResponse (content, tokens, model)
        API->>DB: Save GeneratedReply (attemptNumber=1)
        API->>DB: Record UsageAnalytics(success=true)
        API-->>BG: 200 OK { data: EmailReplyResponse }
        BG-->>Content: { success:true, data }
        Content->>Content: Render AI output in panel
        User->>Content: Clicks "Insert into Reply"
        Content->>Gmail: document.execCommand("insertText", ..., replyText)
    end
```

## Reply Regeneration Flow

Regeneration re-derives the *same* prompt from the persisted `EmailRequest` rather than requiring the client to resend the original payload:

```mermaid
sequenceDiagram
    actor User
    participant UI as React App / Extension
    participant API as Spring Boot API
    participant DB as PostgreSQL
    participant AOAI as Azure OpenAI

    User->>UI: Clicks "Regenerate" on a reply
    UI->>API: POST /api/history/{emailRequestId}/regenerate
    API->>DB: Load EmailRequest (verify ownership)
    API->>DB: SELECT MAX(attemptNumber) WHERE email_request_id = ...
    API->>API: buildPromptForExistingRequest() — dispatches on RequestType
    API->>AOAI: New chat completion call
    AOAI-->>API: New content
    API->>DB: INSERT GeneratedReply (attemptNumber = max + 1)
    Note over DB: Prior attempts are never deleted or overwritten
    API-->>UI: 200 OK { data: EmailReplyResponse }
```
