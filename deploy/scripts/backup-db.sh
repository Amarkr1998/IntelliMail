#!/usr/bin/env bash
# Dumps the Postgres database from the running `postgres` container.
# This is a baseline backup strategy, not disaster-proof - it lives on the
# same disk as the database it's backing up, so it won't survive droplet/disk
# loss. See DEPLOYMENT.md "Backups" for syncing these off-server (e.g. rclone
# to DigitalOcean Spaces or S3), which is a documented next step, not built
# here.
#
# Usage: ./deploy/scripts/backup-db.sh [keep-count, default 14]
set -euo pipefail

cd "$(dirname "$0")/../.."
set -a; source .env; set +a

KEEP="${1:-14}"
BACKUP_DIR="deploy/backups"
mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date -u +%Y%m%d%H%M%S)
OUT_FILE="${BACKUP_DIR}/intellimail-${TIMESTAMP}.dump"

echo "Backing up ${POSTGRES_DB} to ${OUT_FILE}..."
docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB" > "$OUT_FILE"
echo "Backup complete: ${OUT_FILE} ($(du -h "$OUT_FILE" | cut -f1))"

echo "Rotating: keeping the ${KEEP} most recent backups..."
ls -1t "${BACKUP_DIR}"/intellimail-*.dump 2>/dev/null | tail -n +$((KEEP + 1)) | xargs -r rm -v

echo "Done."
