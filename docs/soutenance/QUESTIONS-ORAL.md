# Fiche oral : questions probables et réponses courtes

Chaque réponse tient en 2 ou 3 phrases, avec le fichier à ouvrir si le prof veut voir le code.

## Architecture et consommation des API

**L'API est-elle consommée de façon statique ou dynamique ?**
Dynamique. Angular appelle `/api/...` (chemin relatif, `environment.apiBase`). Nginx relaie vers la Gateway, qui route en `lb://nom-du-service` : l'adresse réelle est trouvée dans Eureka à chaque appel. Aucune adresse de microservice n'est écrite dans le code.
→ `infrastructure/api-gateway/src/main/resources/application.yml`

**Comment un service en appelle un autre ?**
Par OpenFeign (`@FeignClient(name = "ms-user")`), résolu par Eureka et réparti par Spring Cloud LoadBalancer. Un intercepteur de `rescueops-common` transmet le jeton et le `X-Correlation-Id`. Les échanges asynchrones passent par RabbitMQ.

**Pourquoi Eureka ?**
Les services s'enregistrent eux-mêmes avec leur adresse et envoient un battement toutes les 10 s. Sans battement pendant 30 s, ils sont retirés : la Gateway, Feign et Prometheus ne les appellent plus. Une 2e instance est prise en compte sans rien configurer.

## Config Server

**À quoi sert-il ?**
Il centralise la configuration : `config-repo/application.yml` pour tous, `ms-xxx.yml` pour un service. On change un réglage commun à un seul endroit.
→ `infrastructure/config-server`, `config-repo/`

**Que se passe-t-il s'il est arrêté ?**
Le service démarre quand même avec sa configuration locale, grâce au préfixe `optional:` de `spring.config.import`.

**Pourquoi le mode « native » et pas Git ?**
Le dossier `config-repo/` est dans notre dépôt : versionné avec le code, sans second dépôt ni identifiants. Passer à Git = changer une ligne (`spring.cloud.config.server.git.uri`).

**Quelle valeur l'emporte ?**
Celle du Config Server remplace celle de l'`application.yml` du service. C'est pour ça qu'on n'y met que du commun, et jamais de secret.

## Docker

**Comment l'ordre de démarrage est-il garanti ?**
Par `depends_on` avec `condition: service_healthy` : chaque image a un `HEALTHCHECK` sur `/actuator/health`. Un service ne démarre que quand MySQL, RabbitMQ, Keycloak, Eureka et le Config Server répondent.

**Pourquoi deux fichiers compose ?**
`docker-compose.infra.yml` : le socle, pour développer son service dans IntelliJ. `docker-compose.yml` : tout le projet, pour la démo. Le second reprend le premier avec `extends`, sans copier.

**Une image par service ?**
Un seul Dockerfile pour tous les services Java (`docker/java-service.Dockerfile`, argument `MODULE`) : compilation Maven, puis exécution avec un JRE seul et un utilisateur non root.

## Sécurité

**Comment fonctionne l'authentification ?**
Keycloak, OpenID Connect, flux Authorization Code + PKCE (pas de secret dans le navigateur). Chaque requête porte un JWT ; la Gateway et chaque service vérifient sa signature avec les clés publiques de Keycloak, et les rôles avec `@PreAuthorize`.

**Qu'avez-vous durci dans Keycloak ?**
Blocage temporaire après 5 échecs (1 min, puis plus long, 15 min au plus), politique de mot de passe (8 à 64 caractères, majuscule, minuscule, chiffre, spécial, ni l'e-mail ni l'identifiant, pas les 3 derniers), journal des événements de connexion et d'administration.
→ `infrastructure/keycloak/realm-rescueops.json`

**Pourquoi Keycloak ne dit-il pas « compte bloqué » ?**
Pour ne pas renseigner un attaquant : il répond « identifiants invalides » dans tous les cas. Le blocage se voit dans les événements et dans Grafana.

**Pourquoi le rate limit dans Nginx et pas dans la Gateway ?**
Nginx coupe l'excès avant qu'il n'atteigne Java : c'est le plus tôt et le moins coûteux. 20 requêtes/s par IP, rafale de 40, réponse 429 en JSON.
→ `infrastructure/nginx/00-rescueops-http.conf`

**Que fait la CSP ?**
Le navigateur n'exécute que nos scripts (plus deux petits scripts inline autorisés par leur empreinte SHA-256) et ne contacte que nos sources (Google Fonts, tuiles OpenStreetMap, Keycloak). Une injection de script serait bloquée.

**Pourquoi pas de HSTS ?**
HSTS s'applique au nom `localhost` sur tous les ports : il forcerait aussi Keycloak (8180) et `ng serve` (4200) en HTTPS et les casserait. En production, sur un vrai domaine, on l'active (la ligne est en commentaire).

**Le certificat HTTPS ?**
Auto-signé, généré au premier démarrage de Nginx, ou créé par mkcert (autorité locale reconnue par Windows). La clé privée n'est jamais dans Git.

**Où sont les secrets ?**
Dans `.env`, ignoré par Git ; `.env.example` ne contient que des valeurs de développement.

## Load balancer

**Pourquoi Nginx devant deux Gateways, alors que la Gateway répartit déjà ?**
Deux niveaux : Nginx répartit entre les 2 Gateways (round-robin, bascule si l'une tombe) ; chaque Gateway répartit entre les instances des services via Eureka. La Gateway n'est plus un point unique de panne.

**Que se passe-t-il si une Gateway tombe ?**
Nginx l'écarte 10 s (`max_fails=1 fail_timeout=10s`) et rejoue la requête sur l'autre (`proxy_next_upstream`). Les POST ne sont jamais rejoués, pour ne pas créer de doublon.

## Logs, monitoring, traces

**Pourquoi Loki et pas ELK ?**
Loki n'indexe que les étiquettes (service, niveau), pas tout le texte : beaucoup plus léger qu'Elasticsearch sur un PC. Il s'intègre à Grafana, qu'on utilise déjà pour les métriques.

**Pourquoi Alloy et pas Promtail ?**
Promtail est en fin de vie depuis mars 2026 ; Alloy est son successeur officiel chez Grafana. Il lit les logs Docker : aucun microservice n'a été modifié.

**Comment suivre une requête ?**
Nginx crée un `X-Correlation-Id` (ou garde celui du client s'il est sûr), le transmet à la Gateway, qui le propage aux services (et Feign aux appels suivants). Il est dans chaque ligne de log : une recherche dans Grafana retrouve tout le parcours.

**Comment mesurez-vous uptime, heartbeat et ressources ?**
Prometheus lit `/actuator/prometheus` de chaque instance découverte dans Eureka : uptime (`process_uptime_seconds`), CPU, mémoire, threads, requêtes. Le heartbeat est fait par le Blackbox exporter qui appelle `/actuator/health` toutes les 15 s : UP ou DOWN.

**Différence entre logs, métriques et traces ?**
Logs : ce qui s'est passé (texte). Métriques : des chiffres dans le temps (CPU, requêtes/s). Traces (Zipkin) : le chemin et la durée d'une requête à travers les services.

**Pourquoi Zipkin seulement sur 3 services ?**
Ce sont les nôtres (Gateway, ms-user, ms-hospital) : on ne modifie pas les dépendances des services des coéquipiers. Désactivé par défaut (`ZIPKIN_ENABLED`) pour ne pas consommer inutilement.

## Temps réel

**Pourquoi SSE et pas WebSocket ?**
Le bandeau ne fait que recevoir : SSE est unidirectionnel, en HTTP simple, passe par Nginx et la Gateway sans configuration spéciale (hors délais), et se reconnecte facilement. WebSocket serait utile pour un échange dans les deux sens.

**Pourquoi `fetch` et pas `EventSource` côté Angular ?**
`EventSource` ne peut pas envoyer l'en-tête `Authorization` ; or le flux est protégé par le JWT comme le reste de l'API.

**Limite ?**
Avec plusieurs instances de ms-hospital, un navigateur ne reçoit que les changements faits sur son instance. Solution : diffuser l'événement par RabbitMQ (exchange fanout) à toutes les instances.

## Choix technologiques

**Pourquoi RabbitMQ et pas Kafka ?**
Nos échanges sont des événements métier ponctuels entre services (`transfert.confirme`, `mission.terminee`), avec file d'erreurs (DLQ) et nouvelles tentatives. Kafka sert au streaming à gros volume et à la relecture d'historique : inutile ici et lourd en mémoire.

**Et Redis pour le cache ?**
Pas de problème de performance mesuré, donc pas de cache. Si besoin, le premier endroit serait les appels Feign vers `ms-user` (données qui changent peu), avec `@Cacheable`.
