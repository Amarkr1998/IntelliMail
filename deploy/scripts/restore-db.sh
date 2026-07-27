#!/usr/bin/env bash
# Restores a pg_dump (-Fc custom format) produced by backup-db.sh into the
# running `postgres` container. DESTRUCTIVE: drops and recreates the target
# database first.
#
# Usage: ./deploy/scripts/restore-db.sh deploy/backups/intellimail-20260101120000.dump
set -euo pipefail

cd "$(dirname "$0")/../.."
set -a; source .env; set +a

DUMP_FILE="${1:-}"
if [ -z "$DUMP_FILE" ] || [ ! -f "$DUMP_FILE" ]; then
  echo "Usage: $0 <path-to-dump-file>"
  echo "Available backups:"
  ls -1t deploy/backups/intellimail-*.dump 2>/dev/null || echo "  (none found)"
  exit 1
fi

echo "This will DROP and recreate database '${POSTGRES_DB}' before restoring."
read -r -p "Type the database name to confirm: " CONFIRM
if [ "$CONFIRM" != "$POSTGRES_DB" ]; then
  echo "Confirmation did not match. Aborting."
  exit 1
fi

echo "Stopping backend (so it isn't writing during restore)..."
docker compose stop backend

echo "Dropping and recreating ${POSTGRES_DB}..."
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d postgres -c "DROP DATABASE IF EXISTS ${POSTGRES_DB};"
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d postgres -c "CREATE DATABASE ${POSTGRES_DB};"

echo "Restoring from ${DUMP_FILE}..."
docker compose exec -T postgres pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner < "$DUMP_FILE"

echo "Restarting backend..."
docker compose start backend

echo "Restore complete. Check logs with: docker compose logs -f backend"
