#!/bin/sh
# Déclare les exchanges partagés RescueOps via l'API de management (idempotent : PUT).
# Les services les déclarent aussi au démarrage ; ce script garantit leur présence
# dès que le socle est lancé, même avant le premier service.
set -eu
API="http://rabbitmq:15672/api/exchanges/%2F"
AUTH="${RABBITMQ_USER}:${RABBITMQ_PASSWORD}"

# Le broker répond au healthcheck avant que le plugin de management soit prêt : on attend l'API (max 2 min).
i=0
until curl -fsS -o /dev/null -u "$AUTH" "http://rabbitmq:15672/api/overview"; do
  i=$((i + 1))
  if [ "$i" -ge 60 ]; then
    echo "API de management RabbitMQ indisponible après 2 minutes" >&2
    exit 1
  fi
  sleep 2
done

curl -fsS -u "$AUTH" -X PUT -H "content-type: application/json" \
  -d '{"type":"topic","durable":true,"auto_delete":false,"internal":false,"arguments":{}}' \
  "$API/rescueops.events"
curl -fsS -u "$AUTH" -X PUT -H "content-type: application/json" \
  -d '{"type":"direct","durable":true,"auto_delete":false,"internal":false,"arguments":{}}' \
  "$API/rescueops.events.dlx"
echo "Exchanges rescueops.events et rescueops.events.dlx déclarés"
