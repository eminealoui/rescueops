#!/usr/bin/env bash
# =====================================================================
#  Smoke test du socle RescueOps (Gateway Docker -> Eureka -> service sur la machine).
#  Prérequis : docker-compose.infra.yml démarré + echo-service lancé en profil local :
#    java -jar tools/echo-service/target/echo-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=local
# =====================================================================
set -uo pipefail
cd "$(dirname "$0")/.."
GW="${GATEWAY_URL:-http://localhost:8080}"
pass=0; total=6

check() { # check <nom> <condition_ok 0/1> <détail>
  if [ "$2" -eq 0 ]; then echo "  OK   $1"; pass=$((pass + 1)); else echo "  FAIL $1 -> $3"; fi
}

TOKEN=$(bash scripts/get-token.sh coordinateur@rescueops.test) || { echo "Impossible d'obtenir un token"; exit 1; }

# 1. Gateway en vie
body=$(curl -s "$GW/actuator/health")
echo "$body" | grep -q '"UP"'; check "1. Gateway /actuator/health = UP" $? "$body"

# 2. Sans token -> 401
code=$(curl -s -o /dev/null -w '%{http_code}' "$GW/api/hospitals/echo")
[ "$code" = 401 ]; check "2. Sans token -> 401" $? "HTTP $code"

# 3. Avec token -> 200, sub et rôle propagés jusqu'au service
body=$(curl -s -H "Authorization: Bearer $TOKEN" "$GW/api/hospitals/echo")
echo "$body" | grep -q '"sub":"[0-9a-f-]\{36\}"' && echo "$body" | grep -q 'ROLE_COORDINATEUR_HOSPITALIER'
check "3. Avec token -> 200, sub + ROLE_COORDINATEUR_HOSPITALIER reçus par le service" $? "$body"

# 4. En-tête d'usurpation supprimé par la Gateway
body=$(curl -s -H "Authorization: Bearer $TOKEN" -H "X-User-Id: pirate" "$GW/api/hospitals/echo")
echo "$body" | grep -q '"x-correlation-id"' && ! echo "$body" | grep -qi 'x-user-id'
check "4. X-User-Id envoyé par le client n'atteint pas le service" $? "$body"

# 5. Service absent -> 503 ProblemDetail
resp=$(curl -s -w ' HTTP%{http_code}' -H "Authorization: Bearer $TOKEN" "$GW/api/emergencies/x")
echo "$resp" | grep -q 'HTTP503' && echo "$resp" | grep -q '"code":"SERVICE_INDISPONIBLE"'
check "5. Service non démarré -> 503 SERVICE_INDISPONIBLE" $? "$resp"

# 6. Identifiant de corrélation renvoyé
hdr=$(curl -s -D - -o /dev/null -H "Authorization: Bearer $TOKEN" -H "X-Correlation-Id: smoke-12345678" "$GW/api/hospitals/echo")
echo "$hdr" | grep -qi '^x-correlation-id: smoke-12345678'
check "6. X-Correlation-Id renvoyé" $? "$hdr"

if [ "$pass" -eq "$total" ]; then echo "SMOKE OK ($pass/$total)"; else echo "SMOKE KO ($pass/$total)"; exit 1; fi
