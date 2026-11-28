# Scénario de démo : le socle RescueOps (≈ 12 min)

Ordre calé sur la grille : Config Server → Docker Compose → Sécurité → Valeurs ajoutées (load balancer, logs, monitoring, temps réel).

## Avant la séance (la veille, avec une bonne connexion)

```powershell
docker compose build
docker compose --profile monitoring pull
```

Le jour J, 10 minutes avant :

```powershell
docker compose --profile monitoring up -d
docker compose ps
```

Tout doit être `healthy`. Ouvre à l'avance ces onglets :

| Onglet | Adresse |
|---|---|
| Application | https://localhost:8443 |
| Eureka | http://localhost:8761 |
| Config Server | http://localhost:8888/ms-hospital/default |
| Grafana | http://localhost:3000 |
| Zipkin (si activé) | http://localhost:9411 |
| Keycloak (console) | http://localhost:8180 |

Comptes : `coordinateur@rescueops.test` / `Demo1234!` (et les autres rôles, voir le README).

---

## 1. Config Server (1 pt), 2 min

1. Onglet **Config Server** : `http://localhost:8888/ms-hospital/default` montre deux sources : `ms-hospital.yml` (propre au service) et `application.yml` (commun).
2. Dans un terminal :
   ```powershell
   docker compose exec ms-hospital curl -s localhost:8085/actuator/info
   ```
   Réponse : `"config":{"source":"config-server"}` : le service a bien lu sa config au démarrage.
3. **À dire :** « Les réglages communs (heartbeat Eureka, exposition des métriques, chemins publics, interrupteur Zipkin) sont écrits une seule fois dans `config-repo/application.yml`. La ligne `optional:configserver:` permet à un service de démarrer même si le serveur est arrêté. »

## 2. Docker Compose (2 pts), 2 min

1. Montre `docker-compose.yml` : socle repris de `docker-compose.infra.yml` (extends), Config Server, 2 Gateways, 7 services, Angular, load balancer, profil `monitoring`.
2. `docker compose ps` : 22 conteneurs `healthy`.
3. **À dire :** « L'ordre de démarrage est garanti par les healthchecks : MySQL, RabbitMQ, Keycloak, puis Eureka, puis le Config Server, puis les services, puis le front et Nginx. Une seule image Dockerfile sert pour tous les services Java (argument `MODULE`). Seul le load balancer est publié ; les services ne sont joignables que par la Gateway. »
4. Onglet **Eureka** : les 9 applications, dont **API-GATEWAY avec 2 instances**.

## 3. Sécurité (2,5 pts), 3 min

1. **HTTPS** : `http://localhost:8090` redirige vers `https://localhost:8443` (cadenas).
2. **Connexion Keycloak** (OIDC + PKCE) avec le coordinateur ; la page Hôpitaux charge ses données.
3. **Sans jeton** :
   ```powershell
   curl.exe -k https://localhost:8443/api/hospitals
   ```
   → `401` en JSON avec `correlationId`.
4. **Force brute** : 5 mauvais mots de passe sur `apprenant@rescueops.test` → le bon mot de passe est refusé. Dans Grafana (tableau Logs), le compteur « Échecs de connexion Keycloak » monte. Débloquer : console Keycloak → Users → apprenant → onglet *Details* (ou attendre 1 min).
5. **Rate limit** :
   ```powershell
   curl.exe -k -s -o NUL -w "%{http_code}\n" --parallel --parallel-max 100 "https://localhost:8443/api/hospitals?n=[1-150]" | Group-Object | Select-Object Name, Count
   ```
   → une partie en `401`, le reste en `429` ; le corps du 429 est un JSON `TROP_DE_REQUETES`.
6. **En-têtes** : outils du navigateur → Réseau → une réponse : `Content-Security-Policy`, `X-Frame-Options: DENY`, etc.

## 4. Valeurs ajoutées (2 pts), 5 min

### Load balancer et bascule
```powershell
1..6 | ForEach-Object { (curl.exe -k -s -D - -o NUL https://localhost:8443/api/hospitals | Select-String "X-Gateway-Instance").Line }
docker compose stop api-gateway-1
1..4 | ForEach-Object { curl.exe -k -s -o NUL -w "%{http_code} " https://localhost:8443/api/hospitals }
docker compose start api-gateway-1
```
Les réponses alternent entre deux adresses, puis tout passe par la Gateway restante, sans erreur.

### Monitoring : heartbeat, uptime, ressources
Grafana → **RescueOps - Santé des services** : cases UP/DOWN, disponibilité, uptime par instance, CPU, mémoire, threads.
Pendant que `api-gateway-1` est arrêtée (étape précédente), sa case et l'historique passent au rouge, puis reviennent au vert.

### Logs centralisés
```powershell
curl.exe -k -s -o NUL -H "X-Correlation-Id: demo-oral-0001" https://localhost:8443/api/hospitals/abc
```
Grafana → **RescueOps - Logs centralisés** → champ *Rechercher* : `demo-oral-0001` → la ligne Nginx et la ligne de la Gateway de la même requête.

### Traces Zipkin (facultatif)
```powershell
$env:ZIPKIN_ENABLED="true"; docker compose --profile monitoring up -d
```
Naviguer dans la page Hôpitaux, puis Zipkin → *Run query* : une trace `api-gateway → ms-hospital` avec la durée de chaque étape.

### Temps réel (SSE)
Ouvrir la page Hôpitaux (bandeau **EN DIRECT** en haut). Dans un autre onglet, connecté en `operateur@rescueops.test`, créer une demande de transfert : elle apparaît dans le bandeau du premier onglet en moins de 2 s, sans rechargement.

---

## Si quelque chose ne marche pas

| Symptôme | Réponse |
|---|---|
| 502/503 juste après un redémarrage | Normal pendant 30 s : Eureka met à jour son annuaire. |
| Service `unhealthy` | `docker compose logs ms-xxx --tail 50` |
| Alerte certificat | Certificat auto-signé : *Paramètres avancés → Continuer* (ou mkcert, voir le guide). |
| Grafana vide | Vérifier la période (en haut à droite) et que le profil `monitoring` est lancé. |
