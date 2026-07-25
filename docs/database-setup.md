# Database Setup

## 1. Install PostgreSQL

Any PostgreSQL 14+ instance works — local install, a managed cloud instance (Azure Database for PostgreSQL, RDS, etc.), or an existing shared server.

## 2. Create the Database

```bash
psql -U postgres -c "CREATE DATABASE intellimail;"
```

Or, from the `psql` shell:
```sql
CREATE DATABASE intellimail;
```

No manual schema creation is needed beyond this — **Flyway owns the schema** from here.

## 3. Schema Migrations (Flyway)

Migrations live in [`backend/src/main/resources/db/migration/`](../backend/src/main/resources/db/migration/) and run automatically on application startup:

| Migration | Creates |
|---|---|
| `V1__create_roles_table.sql` | `roles` |
| `V2__create_users_table.sql` | `users`, `user_roles` |
| `V3__create_prompt_templates_table.sql` | `prompt_templates` |
| `V4__create_email_requests_table.sql` | `email_requests` |
| `V5__create_generated_replies_table.sql` | `generated_replies` |
| `V6__create_feedback_table.sql` | `feedback` |
| `V7__create_usage_analytics_table.sql` | `usage_analytics` |
| `V8__create_audit_logs_table.sql` | `audit_logs` |
| `V9__seed_roles.sql` | Seeds `ROLE_USER` / `ROLE_ADMIN` |

Flyway tracks applied migrations in its own `flyway_schema_history` table — never edit an already-applied migration file; add a new `V10__...sql` instead.

## 4. Environment-Specific Behavior

- **`dev` profile** (`application-dev.yml`): `ddl-auto: update` — Hibernate can patch the schema for fast local iteration, on top of what Flyway already created.
- **`prod` profile** (`application-prod.yml`): `ddl-auto: validate` — Hibernate only verifies the entity mappings match the schema; **Flyway is the only thing allowed to change the schema in production.**

## 5. Running Tests Without a Real Database

`mvn test` does **not** require PostgreSQL — the test profile (`application-test.yml`) points at an in-memory H2 database with `ddl-auto: create-drop` and Flyway disabled, so the schema is generated directly from the JPA entity mappings for each test run.

## 6. Inspecting the Schema

```bash
psql -U postgres -d intellimail -c "\dt"
```

Expected tables: `roles`, `users`, `user_roles`, `prompt_templates`, `email_requests`, `generated_replies`, `feedback`, `usage_analytics`, `audit_logs`, `flyway_schema_history`.

## 7. Resetting Locally

To start over during development:
```bash
psql -U postgres -c "DROP DATABASE intellimail;"
psql -U postgres -c "CREATE DATABASE intellimail;"
```
Then restart the backend — Flyway will re-run every migration from scratch.
