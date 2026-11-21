#!/usr/bin/env bash
# Vérifie qu'une file durable RabbitMQ survit à « docker compose down / up » (revue finale n°2).
set -uo pipefail
cd "$(dirname "$0")/.."
set -a; . ./.env; set +a
API="http://localhost:15672/api"; AUTH="$RABBITMQ_USER:$RABBITMQ_PASSWORD"
C="docker compose -f docker-compose.infra.yml"
curl -fsS -u "$AUTH" -X PUT -H "content-type: application/json" -d '{"durable":true}' "$API/queues/%2F/persistence-check" >/dev/null
$C down >/dev/null 2>&1; $C up -d rabbitmq >/dev/null 2>&1
for i in $(seq 1 60); do curl -fsS -o /dev/null -u "$AUTH" "$API/overview" 2>/dev/null && break; sleep 2; done
code=$(curl -s -o /dev/null -w '%{http_code}' -u "$AUTH" "$API/queues/%2F/persistence-check")
curl -s -o /dev/null -u "$AUTH" -X DELETE "$API/queues/%2F/persistence-check"
if [ "$code" = 200 ]; then echo "RABBIT PERSISTENCE OK"; else echo "RABBIT PERSISTENCE KO (file perdue, HTTP $code)"; exit 1; fi
