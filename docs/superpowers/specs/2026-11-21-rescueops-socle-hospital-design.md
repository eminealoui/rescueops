# RescueOps : socle partagé, MS User et MS5 Hospital (document de conception)

- Date : 21/11/2026
- Statut : à valider par le porteur du module 5
- Références : `Guide_complet_RescueOps.pdf` (guide de réalisation), `RescueOps_Proposition_Projet_6_Modules_UML.pdf` (fiche de cadrage, BF/BNF, UML), maquette validée `docs/design/maquette-v3.html`

---

## 1. Intention et pourquoi

RescueOps est un projet académique (Architecture Web Distribuée) mené par une **équipe de 6**. Chaque membre développe **un module métier**, sur **sa propre machine**. Trois briques sont **partagées par toute l'équipe** : le Discovery Server, l'API Gateway et MS User (avec Keycloak). Le porteur de ce document construit ces briques partagées **et** son module, MS5 Coordination hospitalière.

Le vrai problème : permettre à 6 personnes, sur 6 machines, de brancher leur service sur un socle commun **sans rien casser, sans reconfigurer Keycloak à la main et sans dériver sur les versions**. Le dépôt d'équipe existant (`eminealoui/rescueops`) n'a qu'un seul commit, en Spring Boot 3.3.5 et PostgreSQL. On **repart de zéro**, sur MySQL.

Attentes explicites du porteur :
- un travail complet, **sans faute**, avec une structure d'expert du domaine ;
- **MySQL**, pas PostgreSQL ;
- un frontend **innovant, premium et pro**, inspiré de TeleTracking (couleurs, interactions, transitions), qui ne ressemble **pas** aux applications générées par IA ;
- un thème **clair par défaut**, avec un **mode sombre** au choix ;
- la couleur de marque **bleu pétrole**.

## 2. Objectifs, non-objectifs, anti-objectifs

**Objectifs**
1. Un dépôt monorepo avec un `pom.xml` parent : mêmes versions pour les 6 membres, Maven Wrapper inclus.
2. Un socle lançable en **une commande** (`docker compose -f docker-compose.infra.yml up -d`) : MySQL, Keycloak (realm importé automatiquement), RabbitMQ, Discovery, Gateway et MS User.
3. Un module `rescueops-common` qui factorise la sécurité JWT, le format d'erreur et l'identifiant de corrélation.
4. MS User complet : profil lié à Keycloak, création automatique au premier accès, administration, tests et OpenAPI.
5. MS5 Hospital complet, avec BF-13, BF-14 et BF-15. Il garantit BNF-05 (**zéro double réservation**) et le **prouve** par un test de concurrence. Il sert de **service de référence** pour l'équipe.
6. Les squelettes prêts à coder des 5 autres services : port, base, sécurité, Eureka, Flyway et un endpoint de test.
7. Le frontend Angular : coquille commune, design system (tokens bleu pétrole, clair et sombre), écrans User et Hospital.
8. Un `CONTRIBUTING.md` qui permet à un coéquipier de démarrer son module en moins de 15 minutes.

**Non-objectifs (pour cette itération)**
- La logique métier des modules 1, 2, 3, 4 et 6 : chaque coéquipier s'en charge.
- Le déploiement en production (HTTPS, gestion des secrets en production) : documenté comme limite.
- Le pattern Outbox : publication des événements après commit seulement, Outbox signalé comme amélioration.
- Kubernetes, et toute infrastructure au-delà de Docker Compose.

**Anti-objectifs (ce qui rendrait le travail raté même s'il « marche »)**
- Une interface qui ressemble à un template IA : dégradés violets, police Inter partout, cartes génériques, Angular Material par défaut.
- Des mots de passe ou des rôles de sécurité stockés dans MySQL.
- Une double réservation possible sous concurrence.
- Un coéquipier obligé de modifier la Gateway, Keycloak ou le POM parent pour démarrer son service.
- Des versions qui divergent entre membres, comme dans le dépôt actuel.
- Des clés étrangères ou des associations JPA entre bases de services différents.

## 3. Contraintes

- **Machine du porteur** : Windows 11, Java 21 (Temurin 21.0.11), Docker 29.7, Node 22.22. **Maven n'est pas dans le PATH** (d'où le Maven Wrapper obligatoire). **MySQL n'est pas installé** (d'où MySQL via Docker).
- **IDE** : IntelliJ pour le backend, VS Code pour Angular.
- **Ports** (guide, inchangés) : Angular 4200, Keycloak 8180, Gateway 8080, Eureka 8761, MS1 8081 … MS7 8087, RabbitMQ 5672/15672. On y ajoute MySQL 3306.
- **Données fictives uniquement** (fiche de cadrage).
- **Rôles** : les 8 rôles du guide, avec Keycloak comme **seule autorité** (§3.5 du guide) : `CITOYEN`, `OPERATEUR`, `GESTIONNAIRE_RESSOURCES`, `AMBULANCIER`, `COORDINATEUR_HOSPITALIER`, `FORMATEUR`, `APPRENANT`, `ADMIN`. Ces noms exacts sont un contrat d'équipe : realm, énumération `Role`, `@PreAuthorize` et guards Angular.
- **Identifiant utilisateur entre services** : **le `sub` Keycloak** (UUID), appelé `keycloakId` dans MS User. Tous les champs `*UserId` des autres services (`demandeurUserId`, `chefUserId`, `titulaireUserId`…) contiennent ce `sub`, car c'est la seule information qu'un service connaît depuis le JWT. L'`id` interne de `user_profile` ne sort jamais de MS User.

## 4. Approches envisagées et choix

| Sujet | Options | Choix et raison |
|---|---|---|
| Découverte de services | Eureka · Consul · DNS Docker sans registre | **Eureka.** Aucune infrastructure en plus, c'est ce qui est évalué (Spring Cloud), et `lb://` permet de démontrer BNF-04. Consul ajoute un conteneur, et le DNS seul perd la répartition de charge. |
| Organisation du code | Monorepo avec POM parent · projets totalement séparés | **Monorepo avec POM parent et sous-modules.** Les versions sont définies une seule fois, et IntelliJ importe tout d'un coup. Les projets séparés sont la cause des versions divergentes du dépôt actuel. |
| Schéma de base | Flyway · `ddl-auto: update` | **Flyway** (scripts versionnés, reproductibles sur 6 machines), avec `ddl-auto: validate`. |
| Stockage | 1 conteneur MySQL avec une base par service · 1 conteneur par service | **Un seul conteneur MySQL 8.4**, une base **et un compte MySQL** par service, chaque compte n'ayant de droits que sur sa base. Plus léger sur un PC étudiant, et l'isolation (BNF-06) reste réelle. |
| Réservation de place (BNF-05) | `UPDATE … WHERE places > 0` atomique · verrou pessimiste · `@Version` optimiste avec relance | **UPDATE conditionnel atomique.** Une seule requête, aucune relance, aucun interblocage. Le verrou pessimiste fonctionne aussi mais bloque. L'optimiste impose de relancer et d'informer l'utilisateur. |
| Création du profil User | Création automatique au 1er `GET /me` · création par un backend via l'API admin Keycloak | **Création automatique au premier accès**, à partir des informations du JWT. Pas de transaction répartie entre Keycloak et MySQL, donc pas de compensation à gérer (§3.4 du guide). L'inscription se fait dans Keycloak (self-registration activée, rôle par défaut CITOYEN). |
| Bibliothèque OIDC Angular | keycloak-angular · angular-oauth2-oidc | **angular-oauth2-oidc** (générique OIDC, PKCE, bien maintenue, sans dépendance au JS Keycloak). *Choix laissé à confirmer au plan selon la compatibilité avec la version d'Angular retenue.* |
| Composants UI | Angular Material · design system maison + Angular CDK | **Design system maison + Angular CDK** (overlays, a11y, drag). Material donnerait le look générique à éviter. |
| Carte | Leaflet + tuiles CARTO (Positron / Dark Matter) · Mapbox · SVG maison | **Leaflet + CARTO**, gratuit avec attribution, avec des tuiles claires ou sombres assorties au thème. |
| Direction visuelle | 3 directions puis 2 fusions, testées en maquettes | **Fusion claire inspirée de TeleTracking, en bleu pétrole** (maquette v3 validée). |

## 5. Architecture

```
Navigateur ── Angular (4200) ──OIDC+PKCE──> Keycloak (8180, realm rescueops)
                 │ Bearer JWT
                 v
           API Gateway (8080) ──> Eureka (8761)
                 │ lb://<service>
   ┌─────────────┼───────────────────────────────┐
 ms-user(8087) ms-hospital(8085)  ms-emergency(8081) … ms-training(8086)
   │              │                    │
 user_db      hospital_db        emergency_db …   (1 conteneur MySQL 8.4, 1 compte par base)
                  └────── RabbitMQ (exchange topic rescueops.events) ──────┘
```

### 5.1 Arborescence du dépôt

```
rescueops/
├── pom.xml                         # parent : versions, dependencyManagement, plugins
├── mvnw, mvnw.cmd, .mvn/
├── rescueops-common/               # bibliothèque partagée (jar, pas une appli)
├── infrastructure/
│   ├── discovery-server/
│   ├── api-gateway/
│   ├── keycloak/realm-rescueops.json
│   └── mysql/init/01-databases.sql
├── services/
│   ├── ms-user/          (8087)   # complet
│   ├── ms-hospital/      (8085)   # complet, service de référence
│   ├── ms-emergency/     (8081)   # squelette
│   ├── ms-resource/      (8082)   # squelette
│   ├── ms-dispatch/      (8083)   # squelette
│   ├── ms-intervention/  (8084)   # squelette
│   └── ms-training/      (8086)   # squelette
├── frontend/rescueops-web/         # Angular
├── docker-compose.infra.yml        # socle partagé (pour les 6 membres)
├── docker-compose.yml              # tout l'environnement (démo / livraison)
├── .env.example                    # variables, sans vrais secrets
├── docs/ (design/, superpowers/specs/, api/, architecture.md)
├── CONTRIBUTING.md
└── README.md
```

Les noms Eureka sont `ms-user`, `ms-hospital`, etc. (`spring.application.name`). Le package racine est `tn.esprit.rescueops.<module>`.

### 5.2 Versions de référence

- **Java 21.**
- **Spring Boot 4.0.x et Spring Cloud 2025.1.x**, la dernière paire compatible, à vérifier sur la matrice officielle au moment du plan. Si une incompatibilité bloque, on se replie sur Spring Boot 3.5.x / Spring Cloud 2025.0.x, ce qui sera signalé dans le plan.
- **Springdoc OpenAPI** : version compatible Boot 4.
- **Base et messagerie** : MySQL 8.4 LTS, `flyway-mysql`, RabbitMQ 4.x (image management), Keycloak 26.x.
- **Tests** : Testcontainers (MySQL, RabbitMQ), JUnit 5, Mockito, Spring Security Test.
- **Frontend** : Angular, dernière version stable compatible avec Node 22.

> Point d'attention pour le builder : Spring Boot 4 découpe les starters (par exemple `spring-boot-starter-webmvc` et un starter Flyway dédié) et passe à Jackson 3. Il faut vérifier les noms exacts des artefacts.

## 6. Socle partagé

### 6.1 `rescueops-common`
Il est fourni en **auto-configuration** Spring Boot : un service n'a qu'à ajouter la dépendance.

**Sécurité**
- `JwtAuthConverter` : lit `realm_access.roles` et produit les autorités `ROLE_<ROLE>`.
- `SecurityFilterChain` par défaut : stateless, resource server JWT, `/actuator/health/**` et la documentation OpenAPI en accès public, tout le reste authentifié.
- Les services ajoutent leurs règles avec `@PreAuthorize`.

**Erreurs**
- `GlobalExceptionHandler` au format **RFC 9457 ProblemDetail** (`type`, `title`, `status`, `detail`, `instance`, `correlationId`, `errors[]` pour la validation).
- Exceptions métier de base : `NotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422), `ForbiddenOperationException` (403).

**Traçabilité (BNF-09)**
- `CorrelationIdFilter` : lit ou crée `X-Correlation-Id`, le place dans le MDC, le renvoie dans la réponse et le propage dans les appels Feign et RabbitMQ.
- Format de log commun, sans jamais journaliser le token.

**Utilitaires**
- `CurrentUser` : extrait le `sub`, l'email et les rôles du JWT.

**Événements**
- `RescueOpsEvent<T>`, un modèle versionné : `eventId` (UUID), `eventType`, `version`, `occurredAt`, `correlationId`, `payload`.
- Configuration de l'exchange topic `rescueops.events`, de la DLQ et du convertisseur JSON.

**Constantes**
- Énumération `Role` avec les 8 rôles.

### 6.2 Discovery Server (8761)
On reprend le travail existant (`@EnableEurekaServer`, auto-préservation désactivée en local, Actuator), en l'alignant sur le POM parent. On ajoute un healthcheck Docker.

### 6.3 API Gateway (8080)
- Spring Cloud Gateway.
- **Les 7 routes sont déclarées dès le départ**. Le préfixe est **conservé** (`/api/hospitals/**` arrive tel quel dans le service) :
  - `/api/users/**` → `lb://ms-user`
  - `/api/hospitals/**`, `/api/transfers/**` → `lb://ms-hospital`
  - `/api/emergencies/**` → `lb://ms-emergency`
  - `/api/resources/**` → `lb://ms-resource`
  - `/api/dispatch/**` → `lb://ms-dispatch`
  - `/api/interventions/**` → `lb://ms-intervention`
  - `/api/trainings/**` → `lb://ms-training`
- **Validation JWT** à l'entrée. Les services valident **aussi** le JWT : la Gateway n'est pas la seule barrière.
- **CORS** restreint à `http://localhost:4200`.
- Filtre de corrélation. Suppression des en-têtes d'identité envoyés par le client.
- Si un service est indisponible, la Gateway renvoie un 503 au format ProblemDetail (BNF-07).
- Agrégation des documentations OpenAPI de tous les services dans une seule interface Swagger, servie par la Gateway.

### 6.4 Keycloak (8180)
Le fichier `realm-rescueops.json` est versionné et importé au démarrage (`--import-realm`). Il contient :
- **Le client `rescueops-web`** :
  - client public, Authorization Code + PKCE S256 ;
  - redirections `http://localhost:4200/*`, web origins `http://localhost:4200`.
- **Les 8 rôles de realm.**
- **Un mapper** qui place les rôles dans `realm_access.roles` de l'access token (comportement par défaut, à vérifier).
- **L'inscription** : self-registration activée, rôle par défaut `CITOYEN`.
- **Un utilisateur de démonstration par rôle** (`citoyen@rescueops.test` … `admin@rescueops.test`). Les mots de passe de démo sont documentés dans le README et marqués comme non sensibles.
- **Le thème des pages Keycloak**. Un thème de connexion aux couleurs RescueOps est **optionnel** : il est prévu en fin de parcours si le temps le permet.

### 6.5 MySQL
- `01-databases.sql` crée les 7 bases (`user_db`, `hospital_db`, `emergency_db`, `resource_db`, `dispatch_db`, `intervention_db`, `training_db`).
- Il crée aussi 7 comptes (`user_svc` …), chacun avec `ALL` sur **sa** base uniquement.
- Les mots de passe sont lus depuis `.env`, avec des valeurs par défaut de développement dans `.env.example`.

### 6.6 Docker Compose
- **`docker-compose.infra.yml`** (utilisé par chaque coéquipier) : MySQL, Keycloak, RabbitMQ, discovery-server, api-gateway et ms-user.
  - Healthchecks, `depends_on: condition: service_healthy`, volumes persistants.
  - Chaque membre lance ensuite **son** service depuis IntelliJ, sur l'hôte. Ce service s'enregistre dans Eureka (dans le conteneur, mais accessible depuis l'hôte).
  - **Point à traiter** : la Gateway, dans Docker, doit pouvoir joindre un service lancé sur l'hôte. Il faut que les services s'enregistrent avec une adresse joignable depuis les conteneurs (`eureka.instance.hostname=host.docker.internal` en profil `local`). Ce point est à valider par un test réel dans le plan.
- **`docker-compose.yml`** : tout l'environnement, frontend compris, pour la démo et la livraison.
- **Profils Spring** :
  - `local` : service sur l'hôte, infrastructure dans Docker, `localhost`.
  - `docker` : tout dans Docker, noms DNS des conteneurs.
  - Le navigateur utilise toujours `localhost:8180` comme émetteur, donc l'`issuer-uri` doit être cohérent entre les deux profils. On **valide la signature via `jwk-set-uri` interne** et on vérifie l'émetteur explicitement (`localhost:8180`).

## 7. MS User (8087)

### 7.1 Modèle (`user_db`)
`user_profile` contient :
- `id` (BINARY(16) UUID) ;
- `keycloak_id` (unique, non nul) ;
- `email` (unique) ;
- `first_name`, `last_name` ;
- `phone` (format E.164 validé) ;
- `address` (facultatif) ;
- `organisation` (facultatif, par exemple le nom de l'hôpital ou de la caserne, pour BNF-02) ;
- `active` (statut métier) ;
- `created_at`, `updated_at` ;
- `version`.

Il **ne contient ni mot de passe ni rôle**.

### 7.2 API

| Méthode et route | Accès | Comportement |
|---|---|---|
| `GET /api/users/me` | authentifié | Renvoie le profil lié au `sub` du JWT. **S'il n'existe pas, il est créé** à partir des informations du JWT (`given_name`, `family_name`, `email`). Opération idempotente, protégée par la contrainte unique sur `keycloak_id` (en cas de course : on capture la violation et on relit). |
| `PUT /api/users/me` | authentifié | Modifie prénom, nom, téléphone, adresse. Le `keycloakId`, l'email, `active` et les rôles ne sont **jamais** modifiables par cet endpoint. |
| `GET /api/users/{keycloakId}` | ADMIN | Profil complet. |
| `GET /api/users?query=&active=&page=&size=&sort=` | ADMIN | Recherche paginée. |
| `PATCH /api/users/{keycloakId}/status` | ADMIN | Active ou désactive le profil (statut métier uniquement). |
| `GET /api/users/{keycloakId}/summary` | authentifié (appels entre services et écrans métier) | Vue minimale `UserSummaryDto` (keycloakId, nom affiché, organisation). Elle permet aux autres services de transformer un `*UserId` en nom. |

Dans toutes les routes, l'identifiant public est le `keycloakId` (le `sub`).

**Rôles** : les rôles d'un utilisateur sont lus dans le **JWT**, pour l'utilisateur courant uniquement. L'attribution des rôles se fait dans la console Keycloak. Gérer les rôles depuis l'application est **hors périmètre** (le guide impose une seule autorité).

### 7.3 Tests obligatoires (§10.2 du guide)
- 401 sans token, 403 avec un rôle insuffisant.
- `/me` crée le profil au premier appel puis le relit.
- Deux appels `/me` simultanés donnent un seul profil.
- Impossible de modifier son `keycloakId`.
- Doublon : 409 au format ProblemDetail, sans fuite d'information.
- 404 cohérent.
- Token expiré ou émetteur incorrect : rejeté.

## 8. MS5 Hospital (8085), service de référence

### 8.1 Modèle (`hospital_db`)

**`hopital`**
- `id`, `nom`, `adresse`, `ville` ;
- `latitude`, `longitude` ;
- `telephone` ;
- `actif` ;
- dates de création et de modification.

**`service_hospitalier`**
- `id`, `hopital_id` (FK interne, même base) ;
- `specialite` : énumération `REANIMATION, TRAUMATOLOGIE, CARDIOLOGIE, PEDIATRIE, NEUROLOGIE, BRULES, URGENCES_GENERALES, MATERNITE` ;
- `capacite_totale`, `places_disponibles` ;
- `actif`, `version` ;
- contrainte `CHECK (places_disponibles BETWEEN 0 AND capacite_totale)`, et unicité de (hopital, specialite).

**`demande_transfert`**
- `id` ;
- `mission_id` (UUID externe venant de MS4, sans FK) ;
- `specialite_requise` ;
- `gravite` : `FAIBLE, MOYENNE, ELEVEE, CRITIQUE` ;
- `latitude_origine`, `longitude_origine` ;
- `service_id` (nul tant qu'aucune place n'est retenue) ;
- `statut` ;
- `expire_at` ;
- `demandeur_user_id`, `traite_par_user_id` ;
- `motif_refus` ;
- dates de création et de modification, `version`.

**`historique_transfert`** (BF-20) : `id`, `demande_id`, `ancien_statut`, `nouveau_statut`, `acteur_user_id`, `horodatage`, `commentaire`.

**`evenement_traite`** : `event_id` (clé primaire), `traite_le`. Sert à l'idempotence des consommateurs (BNF-08).

### 8.2 Machine à états de `DemandeTransfert`

```
DEMANDEE ──reserver(service)──> PLACE_RETENUE ──confirmer──> CONFIRMEE ──admettre──> ADMIS ──sortie──> SORTI
    │                               │  │  │                    │                                (place rendue)
    │                               │  │  └──annuler──> ANNULEE (place rendue)
    │                               │  └──refuser──> REFUSEE   (place rendue)
    │                               └──délai dépassé──> EXPIREE (place rendue)
    ├──annuler──> ANNULEE
    CONFIRMEE ──annuler──> ANNULEE (place rendue, ex. patient décédé ou réorienté)
```

**Cycle de vie d'une place** :
- une place est **prise** au passage à `PLACE_RETENUE` ;
- elle reste prise en `CONFIRMEE` et `ADMIS` ;
- elle est **rendue** à l'une de ces transitions : `REFUSEE`, `EXPIREE`, `ANNULEE` (depuis PLACE_RETENUE ou CONFIRMEE) ou `SORTI`. `SORTI` est la sortie du patient, déclarée par le coordinateur depuis l'écran « Admissions ».
- `places occupées = capacite_totale − places_disponibles`. Il n'y a pas de deuxième compteur, il est déduit.

Le coordinateur peut aussi **corriger manuellement** `places_disponibles` (patients arrivés hors RescueOps), dans la limite de la règle de capacité ci-dessous. La correction est tracée dans l'historique de capacité.
- **Toute transition** qui ne figure pas dans ce schéma renvoie **409**.
- Chaque transition écrit une ligne dans `historique_transfert`.
- Une demande `REFUSEE` ou `EXPIREE` peut être **réessayée** : on crée une nouvelle demande pour la même mission.

**Pourquoi l'état ADMIS** : la fiche de cadrage cite un écran « admissions ». C'est mon choix, à corriger si besoin.

### 8.3 Règles clés
- **Réserver une place (BNF-05)** : `UPDATE service_hospitalier SET places_disponibles = places_disponibles - 1 WHERE id = :id AND actif AND places_disponibles > 0`.
  - Si la requête ne touche aucune ligne, on renvoie 409 `CAPACITE_EPUISEE`.
  - Elle s'exécute dans la **même transaction** que le passage à `PLACE_RETENUE` et le calcul de `expire_at = now + délai`. Le délai est configurable (`rescueops.hospital.reservation-ttl`, 15 min par défaut).
- **Rendre une place** (refus ou expiration) : `UPDATE … SET places_disponibles = places_disponibles + 1 WHERE id = :id AND places_disponibles < capacite_totale`.
  - La transition de la demande se fait avec une condition sur son statut (`WHERE statut='PLACE_RETENUE'`). Une place ne peut donc jamais être rendue deux fois, même si l'expiration et le refus arrivent en même temps.
- **Expiration** : une tâche `@Scheduled` (toutes les 30 s) traite les demandes `PLACE_RETENUE` dont `expire_at < now`.
  - Une demande à la fois, en transaction, avec la transition conditionnelle.
  - Elle reste sûre avec **2 instances** du service (BNF-04) grâce à cette condition, **sans verrou distribué**.
- **Proposer des établissements compatibles (BF-14)**.

  **Candidats** : les services actifs qui ont au moins une place, dans la spécialité requise **ou** en `URGENCES_GENERALES` (repli). Ceux qui sont à plus de 50 km sont exclus (rayon configurable `rescueops.hospital.max-radius-km`).

  **Sous-scores, chacun de 0 à 100** :
  - distance : `100 × (1 − d / rayon_max)`, calculée par Haversine depuis l'origine ;
  - disponibilité : `100 × places_disponibles / capacite_totale` ;
  - spécialité : 100 si c'est la spécialité exacte, 40 si c'est le repli `URGENCES_GENERALES`.

  **Poids selon la gravité** (distance / disponibilité / spécialité) :
  - `CRITIQUE` : 0,6 / 0,1 / 0,3 (le plus proche d'abord) ;
  - `ELEVEE` : 0,5 / 0,2 / 0,3 ;
  - `MOYENNE` et `FAIBLE` : 0,35 / 0,35 / 0,3 (on préserve les services sous tension).

  **Résultat** : un score sur 100, arrondi. La réponse est triée et donne, pour chaque candidat, les sous-scores et une **justification lisible** (par exemple « 4,1 km · 30 % de places libres · spécialité exacte »), conformément aux « recommandations explicables » de la fiche de cadrage. Les poids et le rayon sont dans la configuration.
- **Ajuster une capacité** (coordinateur) : la nouvelle capacité ne peut pas descendre sous le nombre de places déjà occupées ou retenues. Sinon, 422.

### 8.4 API

| Méthode et route | Accès |
|---|---|
| `GET/POST /api/hospitals`, `GET/PUT/DELETE /api/hospitals/{id}` | Lecture : tout utilisateur authentifié. Écriture : COORDINATEUR_HOSPITALIER, ADMIN. La suppression est logique (`actif=false`). |
| `GET/POST /api/hospitals/{id}/services`, `PUT /api/hospitals/services/{serviceId}`, `PATCH …/capacity` | Même découpage lecture / écriture. |
| `GET /api/hospitals/capacities?ville=&specialite=&statut=` | Vue tableau de bord : KPI et liste. Authentifié. |
| `GET /api/hospitals/compatible?specialite=&lat=&lon=&gravite=` | OPERATEUR, AMBULANCIER, COORDINATEUR_HOSPITALIER, ADMIN. |
| `POST /api/transfers` | AMBULANCIER, OPERATEUR (crée une demande en `DEMANDEE`). |
| `POST /api/transfers/{id}/reserve` (body : `serviceId`) | AMBULANCIER, OPERATEUR, COORDINATEUR_HOSPITALIER. |
| `POST /api/transfers/{id}/confirm`, `/refuse` (body : motif), `/admit` | COORDINATEUR_HOSPITALIER, ADMIN. |
| `POST /api/transfers/{id}/cancel` | Demandeur, COORDINATEUR_HOSPITALIER, ADMIN (depuis DEMANDEE, PLACE_RETENUE ou CONFIRMEE). |
| `POST /api/transfers/{id}/discharge` | COORDINATEUR_HOSPITALIER, ADMIN (passage de ADMIS à SORTI, la place est rendue). |
| `GET /api/transfers/feed?since=` | Rôles opérationnels (OPERATEUR, AMBULANCIER, COORDINATEUR_HOSPITALIER, GESTIONNAIRE_RESSOURCES, ADMIN). Derniers changements de statut pour le ticker, sans données personnelles. |
| `GET /api/transfers?statut=&page=`, `GET /api/transfers/{id}` (avec historique) | Rôles hospitaliers et opérationnels. |

### 8.5 Événements (exchange `rescueops.events`)

**Publiés** après commit (`@TransactionalEventListener(AFTER_COMMIT)`) :
- `transfert.place_retenue`
- `transfert.confirme` (cité par le guide)
- `transfert.refuse`
- `transfert.expire`
- `capacite.maj`

**Consommés** : `mission.terminee` (venant de MS4). Si une demande de cette mission est encore en `DEMANDEE` ou `PLACE_RETENUE`, elle est annulée et la place est rendue.
- Le consommateur est idempotent grâce à `evenement_traite`.
- Les messages en échec vont dans la DLQ après 3 tentatives.
- MS4 n'existe pas encore : le consommateur est **testé avec un message simulé**.

### 8.6 Données de démonstration
Un script Flyway `V100__demo_data.sql`, chargé seulement avec le profil `demo`, crée environ **8 hôpitaux fictifs** à Tunis et alentours (noms inspirés d'établissements réels, coordonnées approximatives) avec leurs services.

### 8.7 Tests
- **Unitaires** : machine à états, score et règles de capacité.
- **Web et sécurité** (MockMvc) : 401, 403 et validation, pour chaque endpoint.
- **Intégration** (Testcontainers MySQL) : migrations Flyway et requêtes atomiques.
- **Concurrence (BNF-05)** : 50 threads réservent sur un service qui a 10 places. On vérifie exactement **10 succès, 40 réponses 409** et `places_disponibles = 0`.
- **Course expiration contre refus** sur la même demande : la place n'est rendue qu'une fois.
- **Consommateur** : un même `eventId` reçu deux fois n'a qu'un seul effet.

## 9. Squelettes des 5 autres services
Chacun contient :
- son POM (parent et `rescueops-common`) ;
- son `application.yml` (port, nom Eureka, base, profils `local` et `docker`) ;
- une migration `V1__init.sql` vide commentée ;
- un `PingController` sécurisé ;
- un test de chargement du contexte ;
- son Dockerfile ;
- un README qui renvoie au service de référence MS Hospital.

**Aucune entité métier** : les coéquipiers les écrivent.

## 10. Frontend Angular

### 10.1 Structure
```
src/app/
├── core/       auth (OIDC, guards de rôle, intercepteur JWT, erreurs 401/403/503),
│               api (clients HTTP typés), layout (coquille, barre EN DIRECT, navigation par rôle)
├── design-system/  tokens.css (clair/sombre, marque bleu pétrole), composants :
│               button-pill, kpi-band, stat-card, tag, chip-filter, segmented-tabs,
│               drawer, toast, score-ring, countdown, timeline-steps, data-table, map (Leaflet)
├── features/
│   ├── profile/     mon profil, édition
│   ├── admin-users/ liste paginée, détail, activer/désactiver
│   ├── hospital/    capacités, établissements (CRUD), services, recherche compatible,
│   │                transferts (liste + tiroir de traitement), admissions
│   ├── emergency/ resource/ dispatch/ intervention/ training/   → pages « module en construction »
│   └── errors/      401, 403, 404, service indisponible
└── app.routes.ts   chargement paresseux par feature, guards par rôle
```
- Composants standalone, signals, Reactive Forms typés, `OnPush`.
- Les coéquipiers ajoutent leurs écrans dans `features/<module>` avec les composants du design system.

### 10.2 Design system (maquette v3 validée, à reproduire fidèlement)
- **Couleurs** :
  - fond crème `#f2f0eb`, surfaces blanches ;
  - **marque bleu pétrole** `#0b2f40` (foncé) / `#17698a` (principal) / `#e1edf2` (fond doux) / `#8fd3ec` (accent sur fond foncé) ;
  - statuts : vert `#1e7a52` = disponible, ambre `#d99a00` = réservé, rouge `#c8331f` = saturé ;
  - **la couleur de marque est séparée des couleurs de statut**.
- **Mode sombre** : variables redéfinies sous `[data-theme="dark"]`. Le choix est mémorisé dans le navigateur, et le réglage du système est utilisé par défaut.
- **Typographie** :
  - IBM Plex Sans : interface en 14 px, titres en 300 avec emphase *italique 600*, eyebrows en 11 px majuscules espacées de 2,2 px ;
  - IBM Plex Mono : identifiants, minuteurs, ticker ;
  - Barlow Condensed 700 : gros chiffres.
- **Interactions** reprises de TeleTracking, avec leurs durées :
  - boutons pilule (rayon 50 px), flèche qui glisse au survol ;
  - cartes qui se soulèvent (0,22 s) ;
  - apparitions en fondu vers le haut décalées ;
  - bandeau vert « EN DIRECT » avec ticker défilant, en pause au survol ;
  - compteurs animés, onglets à indicateur glissant ;
  - tiroir latéral avec fond flouté (0,45 s), toasts ;
  - jauges de score circulaires, itinéraire animé sur la carte.
- **Accessibilité** :
  - `prefers-reduced-motion` respecté ;
  - contraste AA dans les deux thèmes ;
  - navigation au clavier, focus visible, Échap ferme le tiroir.
- **Écrans sans maquette** (profil, administration, CRUD hôpitaux et services, recherche compatible, liste des transferts, admissions) : ils sont conçus dans le même style avec les composants du design system. Un **point de contrôle visuel** est prévu : des captures te sont montrées quand les écrans Hospital sont terminés, avant de finaliser.
- **Le ticker EN DIRECT** n'est affiché que pour les rôles opérationnels (voir `/api/transfers/feed`). Il est masqué pour CITOYEN, FORMATEUR et APPRENANT. Il est alimenté par polling REST (15 s).
  - Le temps réel par WebSocket ou SSE est **hors périmètre** de cette itération, signalé comme amélioration.
- Mise en page responsive jusqu'à 375 px.

## 11. Conventions d'équipe (CONTRIBUTING.md)
- **Dépôt** : le monorepo **remplace le contenu de `github.com/eminealoui/rescueops`** (décision du porteur), donc l'équipe garde le même lien.
  - Le remplacement se fait par un **commit normal** sur une branche `develop`, sans force-push ni réécriture de l'historique. L'ancien squelette reste consultable dans l'historique.
  - `main` est mise à jour par une PR.
  - **Rien n'est poussé sans l'accord explicite du porteur**, qui prévient l'équipe avant.
- **Git** : branches `feature/<module>-<sujet>`, PR vers `develop`, `main` protégée ; commits conventionnels.
- **API** :
  - préfixe `/api/<ressource>` en anglais pluriel, corps en camelCase ;
  - dates ISO-8601 en UTC, identifiants UUID ;
  - pagination `page` / `size` / `sort` ;
  - erreurs au format ProblemDetail.
- **Code** : couches `controller / service / repository / domain / dto / mapper / event`. MapStruct pour les mappers, Lombok autorisé sur les DTO et les entités (pas de `@Data` sur les entités).
- **Base** : Flyway `V<n>__<description>.sql`, jamais de modification d'une migration déjà poussée.
- **Secrets** : jamais dans Git (`.env` dans `.gitignore`, `.env.example` versionné).
- **Recette « nouveau module en 15 minutes »** : cloner, `docker compose -f docker-compose.infra.yml up -d`, ouvrir le dossier dans IntelliJ, lancer son service avec le profil `local`, vérifier qu'il apparaît dans Eureka et que la Gateway le route avec un token de démo.

## 12. Ordre de réalisation (demandé par le porteur)
1. **Fondations** : POM parent, Maven Wrapper, `rescueops-common`, MySQL, Keycloak (realm), RabbitMQ, `docker-compose.infra.yml`.
2. **Discovery Server.**
3. **API Gateway.**
4. **MS User.**
5. **MS5 Hospital.**
6. **Squelettes** des 5 autres services, plus le CONTRIBUTING.
7. **Angular** : coquille, design system, écrans User puis Hospital.
8. **`docker-compose.yml`** complet, README, et vérification de bout en bout du parcours de démonstration.

Chaque étape se termine par une **vérification réelle** : build Maven, tests, démarrage, et appel à travers la Gateway avec un vrai token Keycloak.

## 13. Décisions prises par moi, à relire
- Les conventions de route (préfixe conservé, `/api/transfers` en plus de `/api/hospitals`).
- L'état `ADMIS` et le champ `gravite`.
- La formule du score : candidats de repli `URGENCES_GENERALES`, rayon de 50 km, poids qui varient selon la gravité, délai de réservation de 15 minutes.
- L'état `SORTI` (sortie du patient, qui rend la place) et l'annulation possible depuis PLACE_RETENUE et CONFIRMEE.
- Le `sub` Keycloak comme identifiant utilisateur entre services.
- Le ticker réservé aux rôles opérationnels.
- Le polling à 15 s pour le ticker (pas de WebSocket).
- `organisation` dans le profil User.
- La self-registration Keycloak avec le rôle CITOYEN par défaut.
- `angular-oauth2-oidc` plutôt que `keycloak-angular`.
- MapStruct et Lombok.
- Le profil `demo` pour les données fictives.
- Le thème de la page de connexion Keycloak, optionnel.

## 14. Laissé au builder
- Les numéros de version exacts, après vérification des matrices de compatibilité.
- Le découpage des classes à l'intérieur de chaque couche.
- Le nommage précis des DTO.
- Les paramètres des healthchecks.
- Les icônes (jeu d'icônes libre, cohérent, par exemple Lucide).
- La structure interne des composants Angular, tant que l'apparence et les interactions de la maquette v3 sont respectées.
