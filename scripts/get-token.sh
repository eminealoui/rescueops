#!/usr/bin/env bash
# Affiche un access token Keycloak pour un compte de démo (DÉVELOPPEMENT UNIQUEMENT).
#   TOKEN=$(scripts/get-token.sh coordinateur@rescueops.test)
#   curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me
set -euo pipefail
USERNAME="${1:?usage: get-token.sh <email> [mot_de_passe]}"
PASSWORD="${2:-Demo1234!}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"

RESPONSE=$(curl -sS -X POST "$KEYCLOAK_URL/realms/rescueops/protocol/openid-connect/token" \
  --data-urlencode "client_id=rescueops-dev-cli" \
  --data-urlencode "grant_type=password" \
  --data-urlencode "username=$USERNAME" \
  --data-urlencode "password=$PASSWORD")

TOKEN=$(printf '%s' "$RESPONSE" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
if [ -z "$TOKEN" ]; then
  echo "Échec de l'obtention du token : $RESPONSE" >&2
  exit 1
fi
printf '%s\n' "$TOKEN"
