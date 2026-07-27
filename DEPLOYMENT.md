# IntelliMail — Deployment Guide

This is the self-serve deployment package for IntelliMail: Dockerized backend + frontend, Docker Compose orchestration, an Nginx reverse proxy, GitHub Actions CI/CD, and an optional Prometheus/Grafana monitoring stack.

**Scope note**: this package is built and verified locally (Docker on a dev machine). Actually provisioning a cloud server is something you do yourself by following the steps below — no cloud credentials are embedded anywhere here.

## 1. Architecture

```
                                   ┌─────────────────────────┐
  Internet ── :80 (:443 later) ──▶│      reverse-proxy       │
                                   │   (nginx, TLS terminates │
                                   │    here once activated)  │
                                   └───────────┬──────────────┘
                                       /api/*  │  /*
                                 ┌─────────────┴─────────────┐
                                 ▼                           ▼
                          ┌─────────────┐             ┌─────────────┐
                          │   backend   │             │  frontend   │
                          │ Spring Boot │             │ nginx (SPA) │
                          │   :8080     │             └─────────────┘
                          └──────┬──────┘
                                 │
                          ┌──────▼──────┐
                          │  postgres   │
                          │   :5432     │
                          └─────────────┘
```

Only `reverse-proxy` publishes ports to the host (80/443). `postgres`, `backend`, and `frontend` are reachable only on the internal Docker network `intellimail-net` — never exposed directly to the internet. This is deliberate: don't expose a database or a raw application port publicly when a proxy can front it.

## 2. Prerequisites

- Docker Engine 24+ and the Docker Compose plugin (`docker compose version`) — both are already required to work through this guide.
- A GitHub account (for GHCR image hosting and Actions) — you already have one (`Amarkr1998/IntelliMail`).
- Real Azure OpenAI credentials (endpoint, API key, deployment name) — the same ones already used for local development.
- For the DigitalOcean walkthrough: a DigitalOcean account and (eventually, for HTTPS) a domain name you control.

## 3. Local verification (do this first, before touching any cloud server)

```bash
cp .env.example .env
# Edit .env: fill in real POSTGRES_PASSWORD, AZURE_OPENAI_*, JWT_SECRET (generate
# with `openssl rand -base64 64`), and set CORS_ALLOWED_ORIGINS appropriately.

docker compose build
docker compose up -d
docker compose ps          # wait until every service shows "healthy"
docker compose logs -f backend   # watch Flyway migrate a fresh database
```

Then, in a browser: open `http://localhost` (port 80, via the reverse proxy — **not** `localhost:5180`/`:9090`, those are the native dev ports and aren't part of this stack). Register an account, log in, and run one AI action to confirm the whole containerized stack reaches Azure OpenAI correctly.

Tear down when done poking around:
```bash
docker compose down          # stop and remove containers, keep the postgres_data volume
docker compose down -v       # also delete the volume (fresh start / wipes all data)
```

## 4. DigitalOcean droplet setup

1. **Create the droplet**: Ubuntu 24.04 LTS, at least 2 GB RAM (4 GB recommended if you'll also run the monitoring stack), any region close to your users. Note the droplet's public IP.
2. **SSH in and install Docker**:
   ```bash
   ssh root@<droplet-ip>
   curl -fsSL https://get.docker.com | sh
   ```
3. **Clone the repo and configure**:
   ```bash
   git clone https://github.com/Amarkr1998/IntelliMail.git
   cd IntelliMail
   cp .env.example .env
   nano .env   # fill in real values, same as the local verification step
   ```
4. **First launch**:
   ```bash
   docker compose pull   # if using pre-built GHCR images (see CI/CD below)
   # or: docker compose build   # if building on the droplet directly
   docker compose up -d
   docker compose ps
   ```
5. **Firewall**: only open 22 (SSH), 80, and 443. Do **not** open 5432, 8080, or 3000 (Grafana) to the internet — reach those over an SSH tunnel instead (e.g. `ssh -L 3000:localhost:3000 root@<droplet-ip>`).
   ```bash
   ufw allow 22/tcp && ufw allow 80/tcp && ufw allow 443/tcp && ufw enable
   ```

At this point the app is reachable at `http://<droplet-ip>` over plain HTTP.

## 5. CI/CD (GitHub Actions)

`.github/workflows/ci.yml` runs on every push/PR: backend tests (`mvn test`), frontend build, and a Docker build check for both images. No secrets required — this always works.

`.github/workflows/cd.yml` runs on push to `main`: builds and pushes both images to `ghcr.io/amarkr1998/intellimail-{backend,frontend}` (tagged `:latest` and `:<git-sha>`), then tries to SSH into your droplet and run `deploy.sh`. **That deploy job needs three repo secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
|---|---|
| `DEPLOY_HOST` | Your droplet's public IP |
| `DEPLOY_USER` | `root` (or a deploy user you've set up) |
| `DEPLOY_SSH_KEY` | The **private** key half of a keypair whose public half is in the droplet's `~/.ssh/authorized_keys` |

Until you add these, the `deploy` job will fail — that's expected, not a bug; `build-and-push` still runs and publishes images regardless.

**Make the GHCR packages public** after the first push (GitHub → your profile → Packages → each package → Package settings → Change visibility) so `docker compose pull` on the droplet needs no registry login at all. If you'd rather keep them private, run `docker login ghcr.io -u Amarkr1998 -p <PAT with read:packages>` once on the droplet instead — `deploy.sh` never handles that credential itself.

## 6. SSL activation (once you have a domain)

Point your domain's DNS `A` record at the droplet's IP first, and wait for it to propagate (`dig your-domain.com` should show the droplet's IP).

```bash
# In .env, set:
#   DOMAIN_NAME=your-domain.com
#   LETSENCRYPT_EMAIL=you@example.com

./deploy/scripts/init-letsencrypt.sh
```

The script requests a staging certificate first (to avoid burning Let's Encrypt's production rate limit if something's misconfigured), then the real one. Follow its printed instructions to edit `deploy/nginx/nginx.conf` (replace `your-domain.com`, uncomment the two commented server blocks at the bottom) and reload nginx. Add the printed crontab line for automatic renewal.

## 7. Monitoring (optional)

```bash
# In .env, uncomment: MANAGEMENT_SERVER_PORT=9404
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d --force-recreate backend
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up -d
```

Adds Prometheus (scraping the backend's `/actuator/prometheus` on its own internal management port, plus cAdvisor for container-level metrics) and Grafana. `/actuator/prometheus` is explicitly permitted, unauthenticated, in `SecurityConfig.java` — Prometheus can't present a JWT — so its real protection is that this port is never published to the host or proxied by nginx, only reachable by other containers on the internal Docker network; keep it that way. Access Grafana via an SSH tunnel — `ssh -L 3000:localhost:3000 root@<droplet-ip>`, then `http://localhost:3000` (default login `admin` / your `GRAFANA_ADMIN_PASSWORD`). The Prometheus datasource is auto-provisioned; you'll still need to import or build your own dashboards.

The `--force-recreate backend` step matters: Compose doesn't always detect that a bare `.env` edit changed `MANAGEMENT_SERVER_PORT` for an already-running container, so the second command alone can silently leave the old (unset) value in place.

## 8. Backup & restore

```bash
./deploy/scripts/backup-db.sh          # dumps to deploy/backups/, keeps last 14
./deploy/scripts/restore-db.sh deploy/backups/intellimail-<timestamp>.dump
```

This is a **baseline** backup strategy — dumps live on the same disk as the database, so they won't survive droplet/disk loss. For real disaster recovery, sync `deploy/backups/` off-server (e.g. `rclone` to DigitalOcean Spaces or S3) on a cron schedule — not built here, but a natural next step.

Consider adding `0 2 * * * cd ~/IntelliMail && ./deploy/scripts/backup-db.sh` to the droplet's crontab for nightly backups.

## 9. Rollback

```bash
./deploy/scripts/rollback.sh --list                     # see previously deployed shas
./deploy/scripts/rollback.sh --sha <sha>                 # roll back both services
./deploy/scripts/rollback.sh --sha <sha> --service backend   # just one
```

**Important limitation**: this only rewinds application images, never the database schema. Flyway migrations are forward-only (no down-migrations exist in this project). If the deploy you're rolling back away from included a schema-changing migration, `rollback.sh` alone cannot undo that — you'd need `restore-db.sh` from a backup taken before that migration ran.

## 10. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `backend` container keeps restarting | Check `docker compose logs backend`. Usually Postgres wasn't ready in time (shouldn't happen given the `service_healthy` dependency, but check `docker compose logs postgres`) or a missing/wrong env var (`AZURE_OPENAI_*`, `DB_*`). |
| Frontend loads but API calls fail (CORS error in browser console) | `CORS_ALLOWED_ORIGINS` in `.env` doesn't include the origin you're accessing the app from. Since the SPA calls the backend same-origin through the reverse proxy, this usually only bites if you're calling the API directly from a different origin (e.g. the Chrome extension) — make sure that origin is listed. |
| `docker compose pull` fails on the droplet with a 401/403 | GHCR package is private and the droplet hasn't `docker login ghcr.io`'d — see section 5. |
| Let's Encrypt issuance fails | DNS hasn't propagated yet (`dig your-domain.com`), or you're re-running against the production endpoint after hitting the rate limit — wait, or keep testing with `--staging`. |
| `docker compose up` reports `unhealthy` and never recovers | `docker compose logs <service>` first. For `backend`, a cold containerized start (fresh JVM + Flyway running all migrations against a brand-new database) was measured at ~105s in verification — noticeably slower than running natively without container overhead. The healthcheck's `start_period: 180s` gives real margin for this, but a slow/small droplet could still need more; increase it in `docker-compose.yml` if `docker compose logs backend` shows it still starting when the healthcheck gives up. |
| Need to see actuator/Swagger for debugging in prod | Not exposed publicly by design. Use `docker compose exec backend wget -qO- http://localhost:8080/actuator/health` or an SSH tunnel to reach it directly. |

## 11. Security notes

- Postgres, the backend's app port, and (if using monitoring) Grafana/Prometheus/cAdvisor are never published to the host — only the reverse proxy is internet-facing.
- All app containers run as non-root users with `cap_drop: ALL` and `no-new-privileges`, and read-only root filesystems (with narrow `tmpfs` mounts where genuinely needed).
- `.env` (real secrets) is git-ignored, never committed; only `.env.example` (placeholders) is tracked.
- Rotate `JWT_SECRET` and database credentials periodically; both are plain environment variables here (appropriate for a single-VM Compose deployment) — if you outgrow that, Compose's file-based `secrets:` (works outside Swarm too) paired with Spring Boot's `spring.config.import=optional:configtree:/run/secrets/` support is a documented, code-free upgrade path.
