# syntax=docker/dockerfile:1

# ---- Build stage --------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /build

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# Vite bakes import.meta.env.VITE_* values in at BUILD time, not read at
# container start. Default empty -> the SPA calls same-origin relative
# `/api/...` URLs, which the nginx reverse-proxy (see deploy/nginx/nginx.conf)
# forwards to the backend container. Override this build arg only if the
# frontend is ever served from a different origin than the backend.
ARG VITE_API_BASE_URL=""
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
# Google's Sign-In client ID - not a secret, just an identifier, safe to bake
# into the public bundle (same one the backend uses server-side to verify the
# resulting ID token's audience claim).
ARG VITE_GOOGLE_CLIENT_ID=""
ENV VITE_GOOGLE_CLIENT_ID=${VITE_GOOGLE_CLIENT_ID}
RUN npm run build

# ---- Runtime stage --------------------------------------------------------
FROM nginx:1.27-alpine AS runtime

RUN addgroup -S intellimail && adduser -S intellimail -G intellimail \
    && chown -R intellimail:intellimail /var/cache/nginx /var/run \
    && touch /var/run/nginx.pid && chown intellimail:intellimail /var/run/nginx.pid

COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

USER intellimail
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --retries=3 --start-period=5s \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:8080/healthz || exit 1

CMD ["nginx", "-g", "daemon off;"]
