# IntelliMail — AI-Driven Email Automation Platform

IntelliMail helps users write, improve, summarize, translate, and manage emails using Large Language Models (Azure OpenAI via Spring AI), across three surfaces: a Spring Boot REST API, a React web app, and a Chrome extension that brings AI actions directly into Gmail.

```
[Gmail + Chrome Extension]  ─┐
                              ├──►  Spring Boot REST API  ──►  Azure OpenAI (Spring AI)
[React Web App]             ─┘              │
                                             ▼
                                       PostgreSQL
```

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5, Spring AI, Spring Security, Spring Data JPA, PostgreSQL, Flyway, MapStruct, Lombok, springdoc-openapi, JJWT |
| Frontend | React 19, Vite, Material UI, Axios, React Router, React Hook Form |
| Browser Extension | Manifest V3, vanilla JS/HTML/CSS, `chrome.storage` |
| AI | Azure OpenAI (GPT-4o / GPT-4.1) via Spring AI's `ChatClient` |

## Repository Layout

```
Email.java/
├── backend/      Spring Boot API (Java 21, Maven)
├── frontend/     React 19 + Vite web app
├── extension/    Chrome extension (Manifest V3)
├── postman/      Postman collection for the full API
└── docs/         Architecture, ER/sequence diagrams, guides (this folder)
```

See [`docs/project-structure.md`](docs/project-structure.md) for the full package/folder breakdown of each surface.

## Quick Start

1. **Database**: create a PostgreSQL database (see [`docs/database-setup.md`](docs/database-setup.md)).
2. **Backend**: configure environment variables (see [`docs/environment-variables.md`](docs/environment-variables.md)), then:
   ```
   cd backend
   mvn spring-boot:run
   ```
   API available at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.
3. **Frontend**:
   ```
   cd frontend
   npm install
   npm run dev
   ```
   App available at `http://localhost:5173`.
4. **Chrome Extension**: `chrome://extensions` → Developer mode → Load unpacked → select the `extension/` folder. Full steps in [`docs/installation-guide.md`](docs/installation-guide.md).

## Documentation Index

| Document | Contents |
|---|---|
| [`docs/architecture.md`](docs/architecture.md) | System architecture diagram and layer-by-layer explanation |
| [`docs/sequence-diagram.md`](docs/sequence-diagram.md) | End-to-end request flow: Gmail click → AI reply inserted |
| [`docs/er-diagram.md`](docs/er-diagram.md) | Entity-relationship diagram for all 8 database tables |
| [`docs/api-documentation.md`](docs/api-documentation.md) | Full REST API reference (all 20 endpoints) |
| [`docs/installation-guide.md`](docs/installation-guide.md) | Step-by-step setup for backend, frontend, and extension |
| [`docs/environment-variables.md`](docs/environment-variables.md) | Every configurable environment variable, with defaults |
| [`docs/database-setup.md`](docs/database-setup.md) | PostgreSQL setup and Flyway migration workflow |
| [`docs/project-structure.md`](docs/project-structure.md) | Full folder/package structure across all three surfaces |
| [`docs/deployment-guide.md`](docs/deployment-guide.md) | Deploying without Docker (systemd + Nginx) |
| [`docs/future-enhancements.md`](docs/future-enhancements.md) | Known gaps and roadmap ideas |

## Core Features

AI email reply generation, professional/friendly/formal/casual rewriting, grammar correction, summarization, translation, subject line generation, expand/shorten, follow-up emails, meeting requests, thank-you/apology/sales/HR/marketing/cold-outreach generators, custom AI prompts, reply regeneration, response history, favorite replies, reusable prompt templates, and usage analytics.

## Testing

- Backend: JUnit 5 + Mockito unit tests, Spring Boot integration tests (`mvn test` from `backend/`).
- API: Postman collection at [`postman/IntelliMail.postman_collection.json`](postman/IntelliMail.postman_collection.json) — import into Postman and run Register/Login first to populate the collection's auth token.

## License

Proprietary — internal project scaffold. Add a license of your choice before distributing.
