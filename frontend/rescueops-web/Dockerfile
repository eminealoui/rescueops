# syntax=docker/dockerfile:1.7
# =====================================================================
#  Application Angular RescueOps : compilée par Node, servie par Nginx.
#  /api/** est relayé vers la Gateway (même origine, pas de CORS).
# =====================================================================

# --- Étape 1 : compilation ---
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund
COPY . .
RUN npx ng build --configuration production

# --- Étape 2 : service des fichiers statiques ---
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/rescueops-web/browser /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=6 CMD wget -q -O /dev/null http://127.0.0.1/ || exit 1
