# Deployment Guide (Without Docker)

This covers deploying IntelliMail to a single Linux VM (e.g. an Azure VM, EC2 instance, or any bare server) using systemd + Nginx, with the JAR run directly by the JVM — no containers.

## Overview

```
Internet ──► Nginx (443, TLS termination, reverse proxy + static file serving)
                ├── /              → frontend/dist (static React build)
                └── /api, /swagger-ui.html, /api-docs  → Spring Boot (127.0.0.1:8080, systemd service)
                                                              │
                                                              ▼
                                                        PostgreSQL (same VM or managed instance)
```

## 1. Provision the Server

- Ubuntu 22.04 LTS (or similar) with a public IP and DNS pointing at it.
- Install: JDK 21, PostgreSQL 14+, Nginx, Node.js 20+ (only needed to *build* the frontend, not to run it).

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk postgresql nginx
```

## 2. Database

Follow [`database-setup.md`](database-setup.md) on the server, or point `DB_URL` at a managed PostgreSQL instance instead of a local one.

## 3. Build and Deploy the Backend

On your build machine (or directly on the server):
```bash
cd backend
mvn clean package -DskipTests   # skip tests here only if you've already run them in CI
# produces backend/target/intellimail-backend.jar
```

Copy the JAR to the server, e.g. `/opt/intellimail/intellimail-backend.jar`.

**Environment file** — create `/opt/intellimail/intellimail.env` (see [`environment-variables.md`](environment-variables.md) for the full list; **not** committed to git):
```
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/intellimail
DB_USERNAME=intellimail_app
DB_PASSWORD=<strong-password>
AZURE_OPENAI_API_KEY=<your-key>
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com
AZURE_OPENAI_DEPLOYMENT=gpt-4o
JWT_SECRET=<a-long-random-256-bit-secret>
CORS_ALLOWED_ORIGINS=https://your-domain.com
SERVER_PORT=8080
```

**systemd unit** — `/etc/systemd/system/intellimail-backend.service`:
```ini
[Unit]
Description=IntelliMail Backend
After=network.target postgresql.service

[Service]
Type=simple
User=intellimail
EnvironmentFile=/opt/intellimail/intellimail.env
ExecStart=/usr/bin/java -jar /opt/intellimail/intellimail-backend.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
sudo useradd --system --no-create-home intellimail
sudo systemctl daemon-reload
sudo systemctl enable --now intellimail-backend
sudo systemctl status intellimail-backend
```

Backend now listens on `127.0.0.1:8080` (not exposed directly to the internet — Nginx fronts it).

## 4. Build and Deploy the Frontend

```bash
cd frontend
echo "VITE_API_BASE_URL=https://your-domain.com" > .env.production
npm install
npm run build
```

Copy the contents of `frontend/dist/` to e.g. `/var/www/intellimail/`.

## 5. Nginx Configuration

`/etc/nginx/sites-available/intellimail`:
```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # React SPA
    root /var/www/intellimail;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend API + Swagger
    location ~ ^/(api|api-docs|swagger-ui) {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/intellimail /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

Use `certbot` (Let's Encrypt) to obtain the TLS certificate referenced above:
```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

## 6. Chrome Extension for Production

1. Update `extension/manifest.json`'s `host_permissions` to your production API origin (e.g. `https://your-domain.com/*`) instead of `http://localhost:8080/*`.
2. Either distribute as an unpacked extension internally (same "Load unpacked" steps as dev), or package (`chrome://extensions` → **Pack extension**) and publish to the Chrome Web Store for wider distribution.
3. Users set the backend URL once via the popup's **API Settings** panel (defaults to whatever `host_permissions` allows).

## 7. Zero-Downtime Redeploys (Backend)

```bash
mvn clean package -DskipTests
scp target/intellimail-backend.jar server:/opt/intellimail/intellimail-backend.jar.new
ssh server 'mv /opt/intellimail/intellimail-backend.jar.new /opt/intellimail/intellimail-backend.jar && sudo systemctl restart intellimail-backend'
```

For true zero-downtime, run two instances behind Nginx `upstream` with weighted failover and restart one at a time — out of scope for this starter guide but a natural next step (see [`future-enhancements.md`](future-enhancements.md)).

## 8. Health Checks & Monitoring

- `GET /actuator/health` is exposed and unauthenticated — point your uptime monitor at it.
- Logs go to stdout by default under systemd; view with `journalctl -u intellimail-backend -f`.
