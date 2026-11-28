#!/bin/sh
# Certificat HTTPS de localhost, lancé par Nginx avant son démarrage.
#  - si infrastructure/nginx/certs/ contient déjà localhost.pem + localhost-key.pem
#    (par exemple créés par mkcert), ils sont utilisés tels quels ;
#  - sinon un certificat auto-signé gratuit est généré (valable 825 jours) et gardé dans ce dossier.
set -e
DIR=/etc/nginx/certs
if [ -s "$DIR/localhost.pem" ] && [ -s "$DIR/localhost-key.pem" ]; then
  echo "certificats : fichiers existants utilisés"
  exit 0
fi
echo "certificats : génération d'un certificat auto-signé pour localhost"
openssl req -x509 -newkey rsa:2048 -nodes -days 825 \
  -keyout "$DIR/localhost-key.pem" -out "$DIR/localhost.pem" \
  -subj "/CN=localhost/O=RescueOps (developpement)" \
  -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" >/dev/null 2>&1
