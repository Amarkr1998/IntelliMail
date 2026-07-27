# Environment Variables

All variables are read by [`backend/src/main/resources/application.yml`](../backend/src/main/resources/application.yml) with the defaults shown below (dev-friendly defaults; **override every secret before deploying**).

## Database

| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/intellimail` | JDBC connection string |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |

## Azure OpenAI

| Variable | Default | Description |
|---|---|---|
| `AZURE_OPENAI_API_KEY` | *(empty)* | API key from your Azure OpenAI resource — **required** |
| `AZURE_OPENAI_ENDPOINT` | *(empty)* | e.g. `https://your-resource.openai.azure.com` — **required** |
| `AZURE_OPENAI_DEPLOYMENT` | `gpt-4o` | The deployment name (not the base model name) configured in Azure |
| `AZURE_OPENAI_TEMPERATURE` | `0.7` | Default sampling temperature, applied per-request by `AzureOpenAiClient` |
| `AZURE_OPENAI_MAX_TOKENS` | `1024` | Default max response tokens, applied per-request by `AzureOpenAiClient` |
| `AI_SEND_SAMPLING_PARAMETERS` | `true` | Set to `false` for reasoning-tier deployments that reject custom temperature/max-tokens (see below) |

**On `AI_SEND_SAMPLING_PARAMETERS`:** confirmed against a real Azure OpenAI resource — some newer reasoning-tier deployments reject any non-default `temperature` (`"Only the default (1) value is supported"`) and reject the legacy `max_tokens` parameter outright (`"...Use 'max_completion_tokens' instead"`, which Spring AI 1.0.0 doesn't yet expose). If `/api/email/*` calls fail with a 502 and your backend log shows either of those Azure error messages, set `AI_SEND_SAMPLING_PARAMETERS=false` and restart — `AzureOpenAiClient` will send `temperature=1.0` (explicitly overriding Spring AI's own hardcoded `0.7` default) and omit `max_tokens` entirely.

## JWT

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | *(dev placeholder — change in prod)* | HMAC signing key, must be ≥ 256 bits |
| `JWT_ACCESS_EXPIRATION_MS` | `900000` (15 min) | Access token lifetime |
| `JWT_REFRESH_EXPIRATION_MS` | `604800000` (7 days) | Refresh token lifetime |

## CORS

| Variable | Default | Description |
|---|---|---|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,chrome-extension://*` | Comma-separated allowed origins for the React app + extension |

## Server

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | Backend HTTP port |
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` or `prod` (see `application-dev.yml` / `application-prod.yml`) |
| `UPLOAD_MAX_FILE_SIZE` | `10MB` | Max size for `POST /api/email/extract` file uploads (Spring's `DataSize` syntax, e.g. `20MB`) |

## Google Sign-In (multi-tenant SaaS core)

| Variable | Default | Description |
|---|---|---|
| `GOOGLE_OAUTH_CLIENT_ID` | *(empty)* | OAuth 2.0 Web Client ID from Google Cloud Console — see [`saas-setup.md`](saas-setup.md). Not a secret; unset disables the "Sign in with Google" button/endpoint. |

## Stripe Billing (multi-tenant SaaS core)

| Variable | Default | Description |
|---|---|---|
| `STRIPE_SECRET_KEY` | *(empty)* | Secret API key from the Stripe Dashboard — see [`saas-setup.md`](saas-setup.md) |
| `STRIPE_WEBHOOK_SECRET` | *(empty)* | Signing secret for the `/api/billing/webhook` endpoint |
| `STRIPE_PRICE_ID_STARTER` | *(empty)* | Stripe Price ID for the Starter plan |
| `STRIPE_PRICE_ID_PRO` | *(empty)* | Stripe Price ID for the Pro plan |

Organizations still get a 14-day trial subscription row with none of these set — checkout/portal/webhook just have nothing to talk to until they're configured.

## Frontend (`frontend/.env` or `.env.local`)

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend base URL the React app calls |
| `VITE_GOOGLE_CLIENT_ID` | *(empty)* | Same value as `GOOGLE_OAUTH_CLIENT_ID` — baked in at build time |

## Chrome Extension

The extension has no build-time environment variables — the backend URL is configured at runtime via the popup's **API Settings** panel and stored in `chrome.storage.local`. Changing it to a non-localhost domain also requires updating `host_permissions` in `extension/manifest.json` (see [`installation-guide.md`](installation-guide.md)).

## Setting Variables

**Linux/macOS (shell):**
```bash
export DB_PASSWORD=supersecret
export AZURE_OPENAI_API_KEY=sk-...
mvn spring-boot:run
```

**Windows (PowerShell):**
```powershell
$env:DB_PASSWORD = "supersecret"
$env:AZURE_OPENAI_API_KEY = "sk-..."
mvn spring-boot:run
```

**IntelliJ IDEA:** Run Configuration → Environment variables field, or install the EnvFile plugin and point it at a local `.env` file (never commit that file — it's already covered by `backend/.gitignore`).
