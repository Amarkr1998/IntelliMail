#!/usr/bin/env bash
# Pulls the latest built images and restarts the stack, recording what was
# running before the switch so rollback.sh has something to roll back to.
#
# Usage: ./deploy/scripts/deploy.sh
# Run from the repo root, with a populated .env already in place.
set -euo pipefail

cd "$(dirname "$0")/../.."

HISTORY_FILE="deploy/.deploy-history"
mkdir -p deploy
touch "$HISTORY_FILE"

CURRENT_SHA=$(docker inspect --format='{{index .Config.Labels "org.opencontainers.image.revision"}}' ghcr.io/amarkr1998/intellimail-backend:latest 2>/dev/null || echo "unknown")
echo "$(date -u +%Y-%m-%dT%H:%M:%SZ),${CURRENT_SHA}" >> "$HISTORY_FILE"
echo "Recorded pre-deploy state (${CURRENT_SHA}) to ${HISTORY_FILE}"

echo "Pulling latest images..."
docker compose pull

echo "Restarting stack..."
docker compose up -d --remove-orphans

echo "Waiting for services to report healthy..."
for i in $(seq 1 30); do
  UNHEALTHY=$(docker compose ps --format json 2>/dev/null | grep -c '"Health":"unhealthy"' || true)
  STARTING=$(docker compose ps --format json 2>/dev/null | grep -c '"Health":"starting"' || true)
  if [ "$UNHEALTHY" -gt 0 ]; then
    echo "One or more services reported unhealthy - check 'docker compose ps' and 'docker compose logs'."
    exit 1
  fi
  if [ "$STARTING" -eq 0 ]; then
    echo "All services healthy."
    break
  fi
  sleep 5
done

echo "Deploy complete. Verify with: docker compose ps"
