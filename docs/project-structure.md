# Project Structure

## Repository Root

```
Email.java/
├── README.md
├── backend/       Spring Boot API
├── frontend/       React 19 + Vite web app
├── extension/      Chrome extension (Manifest V3)
├── postman/        Postman collection
└── docs/           This documentation set
```

## Backend (`backend/src/main/java/com/intellimail/mail/`)

```
controller/    REST endpoints — Auth, User, Email, History, Templates, Analytics (thin; delegate to service/)
service/       Business logic + transaction boundaries (AuthService, UserService, EmailService, HistoryService,
               PromptTemplateService, AnalyticsService)
repository/    Spring Data JPA interfaces, one per entity, with custom JPQL query methods
entity/        JPA entities — BaseEntity, Role, User, PromptTemplate, EmailRequest, GeneratedReply,
               Feedback, UsageAnalytics, AuditLog
dto/           Request/response records, grouped by feature: auth/, user/, email/, template/, analytics/, common/
mapper/        MapStruct entity <-> DTO mappers (UserMapper, PromptTemplateMapper, EmailMapper, AnalyticsMapper)
config/        Cross-cutting beans — CORS, OpenAPI/Swagger, JPA auditing, ChatClient, typed @ConfigurationProperties
security/      JWT issuing/validation, Spring Security filter chain, UserDetails adapter, RBAC
exception/     GlobalExceptionHandler + all domain exceptions
validation/    Custom Bean Validation constraints (@ValidRewriteStyle, @ValidCustomRequestType)
client/        AzureOpenAiClient — Spring AI ChatClient wrapper (retry, streaming, token accounting)
prompt/        SystemPromptCatalog (20 system prompts) + PromptFactory (prompt templating per RequestType)
logging/       AuditLogRecorder, UsageAnalyticsRecorder — durable side-effect writers
enums/         RoleName, RequestType
util/          ApiResponse envelope, RetryExecutor
```

```
backend/src/main/resources/
├── application.yml, application-dev.yml, application-prod.yml
└── db/migration/     Flyway SQL migrations (V1-V9)

backend/src/test/java/com/intellimail/mail/
    Mirrors the main package structure — repository/, service/, controller/, security/, prompt/, client/,
    validation/, mapper/, exception/, util/, dto/ — unit tests (JUnit 5 + Mockito) and
    Spring Boot integration tests (@SpringBootTest + MockMvc + H2).
```

## Frontend (`frontend/src/`)

```
api/          axiosClient (JWT + auto-refresh interceptor) + one module per resource
              (authApi, userApi, emailApi, historyApi, templateApi, analyticsApi)
context/      AuthContext (session state), SnackbarContext (global toasts)
theme/        ThemeContext (MUI theme + dark mode toggle, persisted)
components/   Navbar, Sidebar, Layout, ProtectedRoute, Loader, EmailEditor, ReplyCard, MarkdownViewer
pages/        LoginPage, RegisterPage, DashboardPage, ComposeAssistantPage, HistoryPage,
              TemplatesPage, AnalyticsPage, SettingsPage, ProfilePage
utils/        requestTypes.js — mirrors the backend's RequestType enum for UI labels/options
```

## Chrome Extension (`extension/`)

```
manifest.json          Manifest V3 config
background/            background.js — service worker; the only place that calls the backend or touches tokens
content/                content.js + content.css — injected into Gmail; floating AI button + panel
popup/                  popup.html/js/css — login, logout, backend URL setting
icons/                  icon16.png, icon48.png, icon128.png
```

## Design Conventions Used Throughout

- **Package-by-layer within the backend, package-by-feature within `dto/`** — enough structure to navigate at this size without over-nesting.
- **Every cross-cutting concern is a package, not scattered code**: `exception/`, `validation/`, `security/`, `logging/`, `config/`.
- **Tests mirror main source 1:1** — a file at `service/EmailService.java` has its test at `service/EmailServiceTest.java` (or an integration-test sibling in `controller/`), so tests are easy to find from the code and vice versa.
