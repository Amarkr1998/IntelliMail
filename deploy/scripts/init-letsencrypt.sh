#!/usr/bin/env bash
# One-time bootstrap for a real Let's Encrypt certificate. Only run this once
# DOMAIN_NAME actually resolves (in DNS) to this server's public IP - Let's
# Encrypt validates domain ownership by fetching a token from this server over
# plain HTTP, so premature runs just fail (and count against the rate limit).
#
# Usage: ./deploy/scripts/init-letsencrypt.sh
set -euo pipefail

cd "$(dirname "$0")/../.."
set -a; source .env; set +a

if [ -z "${DOMAIN_NAME:-}" ] || [ -z "${LETSENCRYPT_EMAIL:-}" ]; then
  echo "Set DOMAIN_NAME and LETSENCRYPT_EMAIL in .env first."
  exit 1
fi

echo "This will request a certificate for: ${DOMAIN_NAME}"
echo "Make sure DNS for ${DOMAIN_NAME} already points at this server's public IP."
read -r -p "Continue? [y/N] " CONFIRM
if [ "$CONFIRM" != "y" ] && [ "$CONFIRM" != "Y" ]; then
  echo "Aborted."
  exit 1
fi

echo "Step 1/3: requesting a staging (test) certificate first, to avoid hitting"
echo "Let's Encrypt's production rate limits if something is misconfigured..."
docker compose run --rm certbot certonly \
  --webroot --webroot-path /var/www/certbot \
  --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email \
  --staging \
  -d "$DOMAIN_NAME"

echo "Staging certificate issued successfully."
echo "Step 2/3: removing the staging cert and requesting the real one..."
docker compose run --rm certbot delete --cert-name "$DOMAIN_NAME" --non-interactive || true
docker compose run --rm certbot certonly \
  --webroot --webroot-path /var/www/certbot \
  --email "$LETSENCRYPT_EMAIL" --agree-tos --no-eff-email \
  -d "$DOMAIN_NAME"

echo "Step 3/3: reloading nginx with the new certificate..."
echo ""
echo "Now do these two things manually (see DEPLOYMENT.md 'SSL activation'):"
echo "  1. In deploy/nginx/nginx.conf, replace every 'your-domain.com' with"
echo "     ${DOMAIN_NAME} and uncomment the two server blocks at the bottom."
echo "  2. docker compose exec reverse-proxy nginx -s reload"
echo ""
echo "Then add this to the host's crontab for automatic renewal:"
echo "  0 3,15 * * * cd $(pwd) && docker compose run --rm certbot renew --quiet && docker compose exec reverse-proxy nginx -s reload"
