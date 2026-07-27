#!/usr/bin/env bash
# Re-pulls a specific previously-deployed image tag and restarts one or both
# app services. Only rewinds application images, NEVER database schema -
# Flyway migrations are forward-only (no down-migrations exist). If the sha
# you're rolling back away from included a schema-changing migration, this
# script cannot safely undo that; use restore-db.sh against a pre-migration
# backup instead.
#
# Usage:
#   ./deploy/scripts/rollback.sh --sha <git-sha> [--service backend|frontend]
#   ./deploy/scripts/rollback.sh --list                # show deploy history
set -euo pipefail

cd "$(dirname "$0")/../.."

HISTORY_FILE="deploy/.deploy-history"
GHCR_OWNER="amarkr1998"
SHA=""
SERVICE=""

while [ $# -gt 0 ]; do
  case "$1" in
    --sha) SHA="$2"; shift 2 ;;
    --service) SERVICE="$2"; shift 2 ;;
    --list)
      echo "Deploy history (timestamp,sha):"
      cat "$HISTORY_FILE" 2>/dev/null || echo "  (no history recorded yet)"
      exit 0
      ;;
    *) echo "Unknown argument: $1"; exit 1 ;;
  esac
done

if [ -z "$SHA" ]; then
  echo "Usage: $0 --sha <git-sha> [--service backend|frontend]"
  echo "Run '$0 --list' to see previously deployed shas."
  exit 1
fi

SERVICES="${SERVICE:-backend frontend}"

for svc in $SERVICES; do
  IMAGE="ghcr.io/${GHCR_OWNER}/intellimail-${svc}:${SHA}"
  echo "Rolling back ${svc} to ${IMAGE}..."
  docker pull "$IMAGE"
  # Retag as :latest - the exact name docker-compose.yml declares for this
  # service, so `up -d --no-deps` below runs this specific sha's image
  # without trying (and failing) to pull ":latest" back over it.
  docker tag "$IMAGE" "ghcr.io/${GHCR_OWNER}/intellimail-${svc}:latest"
  docker compose up -d --no-deps "$svc"
done

echo "Rollback complete. Verify with: docker compose ps"
echo "Reminder: this only rewinds application images. If the previous deploy"
echo "included a database migration, you may also need restore-db.sh."
