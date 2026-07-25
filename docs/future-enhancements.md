# Future Enhancements

## Known Gaps (flagged during the build, not yet addressed)

- **No `Feedback` REST endpoint.** The `feedback` table, entity, and repository exist (Module 2), but no controller/service was built because it wasn't in the original REST API contract. A natural addition: `POST /api/history/replies/{id}/feedback` (`{ rating: 1-5, comment? }`) plus a `GET` for a user's feedback history, and possibly surfacing average rating per `RequestType` in the Analytics endpoint.
- **No admin-only endpoints.** `ROLE_ADMIN` exists and `@EnableMethodSecurity` is wired up (Module 4), but nothing currently requires it — there's no admin dashboard for platform-wide usage, user management, or audit log review. `AuditLogRepository.findByUserIdOrderByCreatedAtDesc` exists but nothing exposes it over REST yet.
- **Streaming responses aren't exposed over HTTP.** `AzureOpenAiClient.generateStream()` (Module 6) returns a `Flux<String>` and is fully implemented, but no controller endpoint uses it yet — the Compose Assistant UI shows the full reply only after generation completes rather than a live-typing effect. Wiring an SSE endpoint (`text/event-stream`) and consuming it from the frontend with `EventSource` would close this gap.
- **No rate limiting.** A user (or a compromised token) can currently call `/api/email/*` as fast as the client allows, with no per-user or per-IP throttle — a real cost concern given each call bills Azure OpenAI tokens. Bucket4j (in-memory or Redis-backed) at the filter level would be the natural fit.

## Roadmap Ideas

- **Refresh token revocation.** Refresh tokens are stateless JWTs (Module 4) — there's no way to invalidate one before it expires (e.g. on "log out everywhere" or a suspected compromise) without maintaining a denylist. A Redis-backed denylist keyed by token `jti` would add this without a schema change.
- **Multi-language UI.** The AI features already support translating *email content* into any language; the web app's own UI strings are English-only. i18next + React would be a standard fit.
- **Team/organization accounts.** Currently every `User` is fully independent — there's no concept of a shared team template library or shared analytics across a company's users, despite `PromptTemplate.isPublic` already hinting at shared-template use cases.
- **Webhook/Outlook support.** The Chrome extension only targets Gmail (`mail.google.com`); an Outlook Web / Outlook desktop add-in would extend the same backend to a second client surface with no backend changes needed.
- **Bundle-size optimization.** The frontend production build currently emits a single ~792 KB JS chunk (Vite warns above 500 KB) — route-based code splitting via `React.lazy()` per page would meaningfully improve first-load time.
- **CI/CD pipeline.** No GitHub Actions / CI config exists yet to run `mvn test` and `npm run build` automatically on push — worth adding once this repository has a real remote.
- **Load testing the AI path.** Retry/backoff behavior (Module 6) has only been unit-tested; a k6 or Gatling script exercising `/api/email/generate` under concurrent load would validate real-world behavior against Azure OpenAI's actual rate limits.

## Explicitly Out of Scope (by design, not oversight)

- **Docker.** The original spec asked for a deployment guide *without* Docker, so systemd + Nginx was used deliberately (see [`deployment-guide.md`](deployment-guide.md)) — containerizing later is straightforward if that changes.
- **Mobile app.** Not part of the original tech stack; the React app is responsive but not a dedicated mobile client.
