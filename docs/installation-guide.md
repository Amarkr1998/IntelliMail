# Installation Guide

## Prerequisites

| Tool | Version | Used for |
|---|---|---|
| JDK | 21 | Backend |
| Maven | 3.9+ | Backend build (or use IntelliJ's bundled Maven) |
| PostgreSQL | 14+ | Database |
| Node.js | 20+ | Frontend + extension tooling |
| Azure OpenAI resource | — | An Azure subscription with a deployed chat model (e.g. `gpt-4o`) |
| Google Chrome | current | Loading the extension |

## 1. Database

See [`database-setup.md`](database-setup.md) for full detail. Short version:

```bash
createdb intellimail
```

## 2. Backend

```bash
cd backend
cp .env.example .env   # if you created one; otherwise export the variables directly (see environment-variables.md)
```

Set the required environment variables (at minimum `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_DEPLOYMENT`, `JWT_SECRET`) — see [`environment-variables.md`](environment-variables.md) for the complete list and defaults.

```bash
mvn clean install       # compiles, runs annotation processing (Lombok/MapStruct), runs tests
mvn spring-boot:run      # starts the API on :8080
```

Flyway runs automatically on startup and creates/updates the schema. Verify it's up:

```bash
curl http://localhost:8080/actuator/health
```

Swagger UI: `http://localhost:8080/swagger-ui.html`

## 3. Frontend

```bash
cd frontend
cp .env.example .env.local   # set VITE_API_BASE_URL if the backend isn't on localhost:8080
npm install
npm run dev
```

App available at `http://localhost:5173`. Register a new account from the UI, or via `POST /api/auth/register` (Postman collection or curl).

**Production build:**
```bash
npm run build     # outputs static assets to frontend/dist/
npm run preview   # serve the production build locally to sanity-check it
```

## 4. Chrome Extension

1. Open `chrome://extensions` in Chrome.
2. Enable **Developer mode** (top-right toggle).
3. Click **Load unpacked**.
4. Select the `extension/` folder (the one containing `manifest.json`).
5. The IntelliMail icon appears in the toolbar. Click it, log in with the same credentials you used on the web app.
6. If your backend isn't at `http://localhost:8080`, expand **API Settings** in the popup and update the backend URL, then click **Save**.

   > If you change the backend URL to a non-`localhost` domain, you must also add that origin to `host_permissions` in `extension/manifest.json` and reload the extension (Chrome enforces this at the manifest level; it cannot be changed purely from the popup).

7. Open Gmail (`https://mail.google.com`), open any email, and click the floating **AI** button in the bottom-right corner.

## Verifying the Full Stack

1. Register via the web app.
2. Go to **Compose Assistant** → **Generate Reply**, paste some email text, click **Generate with AI**. A real call reaches Azure OpenAI — check your Azure usage dashboard if you want to confirm.
3. Check **History** — the request and reply should appear.
4. Check **Analytics** — the request should be counted.
5. Repeat the generation from the Chrome extension on an actual Gmail thread.

## Running Tests

```bash
cd backend
mvn test
```

Tests use an in-memory H2 database (`application-test.yml`) and a stubbed Azure OpenAI key — no live Postgres or Azure OpenAI connection is required to run the test suite.

## Troubleshooting

**`mvn test` fails with `Could not initialize inline Byte Buddy mock maker` / `AgentLoadException: Agent JAR not found`**

Mockito's default mock maker self-attaches a Java agent at runtime. On some JDKs (observed on a JDK 25 build), this self-attach fails outright — and if your Windows username/user profile path contains non-ASCII characters, it can fail specifically with `Agent JAR not found` because the native attach call can't resolve the path to `byte-buddy-agent-*.jar` in your local `.m2` repository. Fix:

1. Copy the resolved `byte-buddy-agent-<version>.jar` (find its path via `mvn dependency:tree | grep byte-buddy-agent`) to a plain-ASCII path, e.g. `C:\Temp\mockito-agent\byte-buddy-agent.jar`.
2. Run tests with that agent attached explicitly:
   ```
   mvn test "-DargLine=-javaagent:C:\Temp\mockito-agent\byte-buddy-agent.jar -XX:+EnableDynamicAgentLoading -Djdk.attach.allowAttachSelf=true"
   ```
`pom.xml` already sets `-XX:+EnableDynamicAgentLoading -Djdk.attach.allowAttachSelf=true` as its default `argLine` (needed on JDK 21+ regardless of the path issue); the `-javaagent` flag above is the additional fix for the non-ASCII-path case specifically, passed via `-DargLine` rather than committed to `pom.xml` since the path is local to your machine.

**`mvn spring-boot:run` fails with `Web server failed to start. Port 8080 was already in use`**

If you're running other local services (another Spring Boot app, a different microservices stack, etc.), port 8080 — and often the whole 8080–8888 range (common Spring Cloud/Eureka/Config Server defaults) — may already be taken. Set `SERVER_PORT` to something clearly outside whatever else is running (check with `netstat -ano | findstr LISTENING` on Windows) and restart — see [`environment-variables.md`](environment-variables.md).
