# RescueOps - Coordination intelligente des urgences

> 👋 **Membre de l'équipe ? Commence par le [guide de l'équipe](docs/GUIDE-EQUIPE.md)** : il te mène à ton guide personnel, étape par étape.

Projet AWD (Architecture Web Distribuée), réalisé par une équipe de 6 :
- **7 microservices Spring Boot** ;
- une **API Gateway**, **Eureka**, **Keycloak**, **MySQL** et **RabbitMQ** ;
- un frontend **Angular**.

## Architecture

```
Navigateur ── Angular (4200) ──OIDC+PKCE──> Keycloak (8180, realm rescueops)
                 │ Bearer JWT
                 v
           API Gateway (8080) ──> Eureka (8761)
                 │ lb://<service>
   ms-user (8087) · ms-hospital (8085) · ms-emergency (8081) · ms-resource (8082)
   ms-dispatch (8083) · ms-intervention (8084) · ms-training (8086)
                 │                                    │
   MySQL (1 conteneur, 1 base + 1 compte par service)  RabbitMQ (exchange rescueops.events)
```

| Composant | URL | Remarque |
|---|---|---|
| API Gateway | http://localhost:8080 | point d'entrée unique |
| Swagger (tous les services) | http://localhost:8080/swagger-ui.html | |
| Eureka | http://localhost:8761 | services enregistrés |
| Keycloak (console) | http://localhost:8180 | identifiants dans `.env` |
| RabbitMQ (console) | http://localhost:15672 | identifiants dans `.env` |
| MySQL | `localhost:3307` | **3307** côté machine, pour éviter le conflit avec un MySQL Windows sur 3306 |

## Prérequis
- Java 21 (Temurin)
- Docker Desktop (démarré)
- Git Bash (Windows) pour les scripts `.sh`
- Node.js 22 (frontend)
- **Maven n'est pas nécessaire** : utiliser `./mvnw` (Git Bash) ou `mvnw.cmd` (PowerShell).

## Démarrer le socle partagé (tous les membres)

```bash
cp .env.example .env                                   # une seule fois
docker compose -f docker-compose.infra.yml up -d --build
docker compose -f docker-compose.infra.yml ps          # tout doit être "healthy"
```

Cette commande démarre :
- MySQL (avec ses 7 bases) ;
- Keycloak, avec le realm `rescueops` importé automatiquement ;
- RabbitMQ ;
- Eureka et l'API Gateway.

## Développer son microservice (IntelliJ)

1. Le socle tourne (commande ci-dessus).
2. Ouvrir le dossier `rescueops/` dans IntelliJ. Il est importé comme un projet Maven avec tous les modules.
3. Lancer **son** service avec le profil Spring `local`. La recette de `application-local.yml` est la suivante :

```yaml
eureka:
  client.service-url.defaultZone: http://localhost:8761/eureka/
  instance:
    hostname: host.docker.internal   # la Gateway (dans Docker) joint votre machine
    prefer-ip-address: false
rescueops.security.jwk-set-uri: http://localhost:8180/realms/rescueops/protocol/openid-connect/certs
spring.datasource.url: jdbc:mysql://localhost:3307/<votre>_db
```

Et dans `application.yml` (tous profils), pour avoir l'identifiant de corrélation dans les logs :

```yaml
spring.config.import: classpath:rescueops-logging.properties
```

Besoin de **votre propre** `SecurityFilterChain` ? Terminez-la par `RescueOpsSecurity.applyDefaults(http, props).build()`. Sinon, vous perdez les rôles Keycloak et toutes les règles `hasRole(...)` renvoient 403. Pour seulement ajouter des chemins publics, utilisez la propriété `rescueops.security.public-paths`.

4. Le service apparaît dans Eureka. La Gateway le route **sans aucune modification** : les 7 routes sont déjà déclarées.

| Service | Préfixe d'API | Base | Compte MySQL |
|---|---|---|---|
| ms-user | `/api/users/**` | `user_db` | `user_svc` |
| ms-hospital | `/api/hospitals/**`, `/api/transfers/**` | `hospital_db` | `hospital_svc` |
| ms-emergency | `/api/emergencies/**` | `emergency_db` | `emergency_svc` |
| ms-resource | `/api/resources/**` | `resource_db` | `resource_svc` |
| ms-dispatch | `/api/dispatch/**` | `dispatch_db` | `dispatch_svc` |
| ms-intervention | `/api/interventions/**` | `intervention_db` | `intervention_svc` |
| ms-training | `/api/trainings/**` | `training_db` | `training_svc` |

## Comptes de démonstration (données fictives, non sensibles)

Mot de passe commun : `Demo1234!`

| Compte | Rôle |
|---|---|
| `citoyen@rescueops.test` | CITOYEN |
| `operateur@rescueops.test` | OPERATEUR |
| `gestionnaire@rescueops.test` | GESTIONNAIRE_RESSOURCES |
| `ambulancier@rescueops.test` | AMBULANCIER |
| `coordinateur@rescueops.test` | COORDINATEUR_HOSPITALIER |
| `formateur@rescueops.test` | FORMATEUR |
| `apprenant@rescueops.test` | APPRENANT |
| `admin@rescueops.test` | ADMIN |

Tous les comptes ont aussi le rôle `CITOYEN` (rôle par défaut du realm).

### Obtenir un token (Postman, curl)

```bash
TOKEN=$(scripts/get-token.sh coordinateur@rescueops.test)
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/users/me
```

En PowerShell : `$TOKEN = .\scripts\get-token.ps1 coordinateur@rescueops.test`

Le client `rescueops-dev-cli` (grant password) sert **uniquement au développement**. Le frontend utilise `rescueops-web` (Authorization Code + PKCE).

## Vérifier le socle

```bash
./mvnw install                                                         # tests de tous les modules + installation
./mvnw -f tools/echo-service package -DskipTests                       # faux service de test (hors modules)
java -jar tools/echo-service/target/echo-service-0.1.0-SNAPSHOT.jar --spring.profiles.active=local &
bash scripts/smoke-infra.sh                                            # attendu : SMOKE OK (6/6)
bash scripts/check-rabbit-persistence.sh                               # attendu : RABBIT PERSISTENCE OK
```

Le smoke test vérifie six points :
1. La Gateway est en vie.
2. Sans token, la réponse est 401.
3. Le token est propagé jusqu'au service.
4. L'en-tête d'usurpation `X-User-Id` est supprimé.
5. Un service absent renvoie un 503 propre.
6. L'identifiant de corrélation est renvoyé.

## Conventions communes (fournies par `rescueops-common`)

Un service qui ajoute la dépendance `rescueops-common` obtient automatiquement :
- **La sécurité** : validation du JWT Keycloak (signature, dates et émetteur), et rôles `ROLE_<NOM>` utilisables dans `@PreAuthorize("hasRole('ADMIN')")`.
- **L'utilisateur courant** : `CurrentUser.current()` (`keycloakId` = le `sub`, identifiant partagé entre services).
- **Les erreurs** au format RFC 9457 ProblemDetail, avec un `code` stable et un `correlationId`, sans fuite d'information SQL.
- **Les exceptions métier** prêtes à l'emploi : `NotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422) et `ForbiddenOperationException` (403).
- **Les événements** `RescueOpsEvent` sur l'exchange `rescueops.events`. Starter à utiliser : `spring-boot-starter-rabbitmq`.
- **OpenAPI** avec authentification Bearer.

## Dépannage

| Symptôme | Cause probable |
|---|---|
| `Bind for 0.0.0.0:3306 failed` | Un MySQL Windows occupe le port : on utilise `MYSQL_HOST_PORT=3307` (déjà par défaut). |
| La Gateway renvoie 503 pour mon service | Le service n'est pas UP dans Eureka, ou il est lancé sans le profil `local` (hostname `host.docker.internal`). |
| 401 avec un token valide | Token obtenu sur une autre URL que `http://localhost:8180` (l'émetteur doit correspondre). |
| Realm modifié mais non pris en compte | L'import ne se fait qu'au premier démarrage. Pour repartir de zéro : `docker compose -f docker-compose.infra.yml down -v` (**efface les données**). |
