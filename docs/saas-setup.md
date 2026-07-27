# Multi-Tenant SaaS Setup (Organizations, Google Sign-In, Stripe Billing)

This covers the two external services the multi-tenant SaaS core depends on. Both are optional at the app level: with no `GOOGLE_OAUTH_CLIENT_ID`/`STRIPE_*` set, the app runs exactly as before — organizations, invitations, and RBAC still work, "Sign in with Google" and billing checkout simply have nothing to talk to.

## 1. Google Sign-In (OAuth 2.0 ID-token login)

This is **not** the Gmail API — no inbox access, no consent-screen scope review, no client secret. It's the standard "Sign in with Google" button: the frontend gets a signed ID token directly from Google, and the backend verifies it against Google's public keys. One client ID, no secret, nothing server-to-server.

1. Go to the [Google Cloud Console](https://console.cloud.google.com/) and create a project (or select an existing one).
2. **APIs & Services → OAuth consent screen**: choose **External**, fill in the required app name/support email fields. The default scopes (`openid`, `email`, `profile`) need no review — you can publish immediately or keep it in Testing with your own account added as a test user.
3. **APIs & Services → Credentials → Create Credentials → OAuth client ID**:
   - Application type: **Web application**
   - **Authorized JavaScript origins**: add every origin the frontend is served from, e.g. `http://localhost:5173` for local dev and `https://your-domain.com` for production.
   - **Authorized redirect URIs**: leave empty — the ID-token flow never redirects back to the backend.
4. Copy the generated **Client ID** (looks like `1234567890-abc...apps.googleusercontent.com`). There is no client secret to copy — this app never uses one.
5. Set it in both places (it's the same value, used client-side by the button and server-side to verify the token's `aud` claim):
   - `GOOGLE_OAUTH_CLIENT_ID` (backend — `.env` for native dev, root `.env` for Docker Compose)
   - `VITE_GOOGLE_CLIENT_ID` (frontend — `frontend/.env.local` for native dev; baked in at Docker build time via the `docker-compose.yml` `frontend.build.args`, sourced from the same `GOOGLE_OAUTH_CLIENT_ID`)

Restart the backend (and rebuild the frontend, if the client ID changed) — the "Sign in with Google" button on Login/Register will start working.

## 2. Stripe Billing

Flat-rate subscription tiers only (Starter/Pro) — no usage-based/metered billing. Stripe's own hosted Checkout and Customer Portal pages handle payment methods, invoices, and plan changes; this app never renders or stores card details.

1. Create a [Stripe account](https://dashboard.stripe.com/register) (or use an existing one). Stay in **Test mode** (toggle top-right of the Dashboard) until you're ready to go live.
2. **Products → Add product**, once for each plan:
   - "Starter" — add one recurring **Price** (e.g. monthly). Copy the resulting Price ID (`price_...`).
   - "Pro" — same, its own Price ID.
3. **Developers → API keys**: copy the **Secret key** (`sk_test_...` in test mode, `sk_live_...` once you switch to live mode — never the publishable key, this app has no frontend Stripe SDK).
4. **Developers → Webhooks → Add endpoint**:
   - Endpoint URL: `https://<your-domain>/api/billing/webhook`
   - Events to send: `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`
   - Copy the endpoint's **Signing secret** (`whsec_...`).
5. Set all four values:
   - `STRIPE_SECRET_KEY`
   - `STRIPE_WEBHOOK_SECRET`
   - `STRIPE_PRICE_ID_STARTER`
   - `STRIPE_PRICE_ID_PRO`

### Local webhook testing

Stripe can't reach `localhost` directly. Use the [Stripe CLI](https://stripe.com/docs/stripe-cli):

```bash
stripe login
stripe listen --forward-to localhost:9090/api/billing/webhook
```

This prints a webhook signing secret starting with `whsec_` — use that one in your local `.env` instead of the Dashboard's, since the CLI signs events with its own ad-hoc secret for the forwarded session.

### How it fits together

- Every organization gets a `TRIALING` subscription automatically when created (14-day trial, no Stripe API call yet).
- The **first** checkout lazily creates a Stripe Customer and starts a Checkout Session in subscription mode.
- "Manage billing" opens the Stripe-hosted Customer Portal (plan changes, cancellation, invoice history — nothing custom-built here).
- The webhook keeps the local `subscriptions` table in sync (`ACTIVE`/`PAST_DUE`/`CANCELED`) as events arrive; handlers are idempotent (always a full upsert keyed on the Stripe subscription ID), so redelivered events are safe.
- Once a trial expires with no active paid plan, the org's AI-generation endpoints (compose, voice) return `403` until they upgrade — enforced server-side via `@PreAuthorize("@subscriptionGuard.hasActiveAccess(authentication)")`, not just a frontend check. Solo (org-less) users are entirely unaffected by any of this.
