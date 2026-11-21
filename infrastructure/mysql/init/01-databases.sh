#!/bin/bash
# =====================================================================
#  Création des 7 bases RescueOps et d'UN compte MySQL par microservice.
#  Chaque compte n'a des droits que sur SA base (BNF-06 : aucune lecture
#  croisée entre services). Exécuté une seule fois, au premier démarrage
#  du volume MySQL. Les mots de passe viennent de .env.
# =====================================================================
set -euo pipefail

create_db() {
  local db="$1" user="$2" password="$3"
  if [ -z "$password" ]; then
    echo "Mot de passe manquant pour $user (voir .env.example)" >&2
    exit 1
  fi
  mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS \`${db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '${user}'@'%' IDENTIFIED BY '${password}';
GRANT ALL PRIVILEGES ON \`${db}\`.* TO '${user}'@'%';
SQL
  echo "Base ${db} prête (compte ${user})"
}

create_db user_db         user_svc         "${USER_DB_PASSWORD:-}"
create_db hospital_db     hospital_svc     "${HOSPITAL_DB_PASSWORD:-}"
create_db emergency_db    emergency_svc    "${EMERGENCY_DB_PASSWORD:-}"
create_db resource_db     resource_svc     "${RESOURCE_DB_PASSWORD:-}"
create_db dispatch_db     dispatch_svc     "${DISPATCH_DB_PASSWORD:-}"
create_db intervention_db intervention_svc "${INTERVENTION_DB_PASSWORD:-}"
create_db training_db     training_svc     "${TRAINING_DB_PASSWORD:-}"

mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "FLUSH PRIVILEGES;"
