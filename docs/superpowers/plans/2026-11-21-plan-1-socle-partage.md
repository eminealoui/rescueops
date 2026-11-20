# Plan 1 : socle partagé (fondations, Discovery, Gateway, Keycloak, MySQL)

> **Pour les agents qui exécutent ce plan :** SOUS-SKILL REQUIS : utiliser superpowers:subagent-driven-development (recommandé) ou superpowers:executing-plans pour exécuter ce plan tâche par tâche. Les étapes utilisent la syntaxe à cocher (`- [ ]`) pour le suivi.

**But :** livrer le socle que les 6 membres de l'équipe lancent en une commande. Il comprend :
- le POM parent et le Maven Wrapper ;
- `rescueops-common` ;
- MySQL (7 bases isolées) ;
- Keycloak (realm importé) ;
- RabbitMQ ;
- le Discovery Server ;
- l'API Gateway (7 routes protégées par JWT).

**Architecture :** monorepo Maven. `rescueops-common` est une bibliothèque d'auto-configuration utilisée par tous les services servlet. La Gateway (réactive) a sa propre configuration de sécurité. Toute l'infrastructure tourne dans `docker-compose.infra.yml`. Les services d'un membre tournent sur l'hôte (profil `local`) ou dans Docker (profil `docker`).

**Stack technique :**
- Java 21, Spring Boot **4.2.0**, Spring Cloud **2026.0.0**, springdoc **3.2.1**, MapStruct **1.6.3** ;
- MySQL **8.4.13**, Keycloak **26.8.2**, RabbitMQ **4.3.8-management** ;
- Maven **3.9.16** via le Maven Wrapper **3.3.4**.

**Spec :** `docs/superpowers/specs/2026-11-21-rescueops-socle-hospital-design.md`

**Découpage du projet complet en 5 plans** (la spec couvre plusieurs sous-systèmes) :

| Plan | Contenu |
|---|---|
| **1 (ce plan)** | Socle partagé |
| 2 | MS User |
| 3 | MS Hospital |
| 4 | Squelettes des 5 services, CONTRIBUTING, `docker-compose.yml` complet |
| 5 | Frontend Angular (design system, User, Hospital) |

Chaque plan produit un logiciel qui marche et qui se teste seul.

## Contraintes globales

- Racine du dépôt : `C:\Users\Dell\Downloads\discovery-server\rescueops`. Le package racine est `tn.esprit.rescueops`, le groupId `tn.esprit.rescueops`, la version `0.1.0-SNAPSHOT`.
- Ports :
  - Gateway 8080, Eureka 8761, Keycloak 8180 (conteneur 8080), MySQL 3306, RabbitMQ 5672/15672 ;
  - ms-emergency 8081, ms-resource 8082, ms-dispatch 8083, ms-intervention 8084, ms-hospital 8085, ms-training 8086, ms-user 8087.
- Noms Eureka : `ms-user`, `ms-hospital`, `ms-emergency`, `ms-resource`, `ms-dispatch`, `ms-intervention`, `ms-training`, `api-gateway`, `discovery-server`.
- Les 8 rôles (contrat d'équipe, orthographe exacte) : `CITOYEN`, `OPERATEUR`, `GESTIONNAIRE_RESSOURCES`, `AMBULANCIER`, `COORDINATEUR_HOSPITALIER`, `FORMATEUR`, `APPRENANT`, `ADMIN`. Ils sont lus depuis `realm_access.roles` et deviennent les autorités `ROLE_<NOM>`.
- L'émetteur JWT attendu est **toujours** `http://localhost:8180/realms/rescueops` (Keycloak lancé avec `KC_HOSTNAME=http://localhost:8180`). Les JWKS sont lus sur une URL configurable (`localhost:8180` en profil `local`, `keycloak:8080` en profil `docker`).
- Les erreurs HTTP suivent le format RFC 9457 `ProblemDetail`, avec en plus les propriétés `correlationId` et, pour la validation, `errors` (liste de `{field, message}`).
- En-tête de corrélation : `X-Correlation-Id`. Clé MDC : `correlationId`.
- Exchange RabbitMQ : `rescueops.events` (topic), DLX `rescueops.events.dlx`.
- Aucun secret réel dans Git : `.env` est ignoré, `.env.example` est versionné avec des valeurs de développement.
- La base est gérée par Flyway, avec `spring.jpa.hibernate.ddl-auto=validate` (pour les plans suivants).
- Aucun `git push` sans l'accord explicite du porteur.

## Points de vigilance

1. **Émetteur différent entre le navigateur et Docker.** Un token obtenu sur `localhost:8180` doit être accepté par la Gateway qui tourne dans Docker. La Tâche 6 le teste de bout en bout.
2. **Service lancé dans IntelliJ, appelé par la Gateway qui tourne dans Docker.** La route doit atteindre l'hôte. La Tâche 6 le teste avec un service factice sur l'hôte, enregistré avec `host.docker.internal`.
3. **Usurpation d'identité par en-tête.** Un client qui envoie `X-User-Id` ou `X-User-Roles` ne doit jamais voir ces en-têtes arriver aux services. Testé en Tâche 5.
4. **Isolation des comptes MySQL.** `user_svc` ne doit pas pouvoir lire `hospital_db`. Testé en Tâche 3.
5. **Redémarrage du socle.** Après `down` puis `up` (sans `-v`), les données et le realm sont conservés, et le realm n'est pas réimporté en écrasant les données. Testé en Tâche 6.

---

### Task 1 : initialisation du dépôt, POM parent et Maven Wrapper

**Fichiers :**
- Créer : `pom.xml`, `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd`, `.gitignore`, `.gitattributes`, `.editorconfig`, `.env.example`, `README.md` (squelette)

**Interfaces :**
- Produit : le parent `tn.esprit.rescueops:rescueops-parent:0.1.0-SNAPSHOT` (packaging `pom`), qui hérite de `spring-boot-starter-parent:4.2.0`.
  - Il importe `spring-cloud-dependencies:2026.0.0`.
  - Il gère les versions de `springdoc-openapi-starter-webmvc-ui` et `-webflux-ui` (3.2.1), `mapstruct` / `mapstruct-processor` (1.6.3) et `rescueops-common` (`${project.version}`).
  - Il configure `maven-compiler-plugin` avec les annotation processors Lombok puis MapStruct (dans cet ordre, avec `lombok-mapstruct-binding`).
  - `<modules>` : `rescueops-common`, `infrastructure/discovery-server`, `infrastructure/api-gateway`. Les plans suivants ajoutent les services.

- [ ] **Étape 1 :** `git init`. Créer `.gitignore` (target/, .idea/, *.iml, node_modules/, dist/, .angular/, .env, .superpowers/, *.log) et `.gitattributes` (`* text=auto`, `mvnw text eol=lf`, `*.cmd text eol=crlf`).
- [ ] **Étape 2 :** télécharger `maven-wrapper-distribution-3.3.4-only-script.zip` depuis Maven Central dans le scratchpad, en extraire `mvnw` et `mvnw.cmd` à la racine. Écrire `.mvn/wrapper/maven-wrapper.properties` avec `distributionType=only-script` et `distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip`.
- [ ] **Étape 3 :** écrire `pom.xml` (parent décrit ci-dessus, `java.version=21`, encodage UTF-8).
- [ ] **Étape 4 :** écrire `.env.example` avec : `MYSQL_ROOT_PASSWORD`, `USER_DB_PASSWORD`, `HOSPITAL_DB_PASSWORD`, `EMERGENCY_DB_PASSWORD`, `RESOURCE_DB_PASSWORD`, `DISPATCH_DB_PASSWORD`, `INTERVENTION_DB_PASSWORD`, `TRAINING_DB_PASSWORD`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`, `RABBITMQ_USER`, `RABBITMQ_PASSWORD`. Les valeurs sont `dev_<nom>`, avec un commentaire « développement uniquement ».
- [ ] **Étape 5 : vérifier.** Lancer `./mvnw -v`. Résultat attendu : `Apache Maven 3.9.16` et `Java version: 21`.
- [ ] **Étape 6 :** commit `chore: initialise le monorepo RescueOps (POM parent, Maven Wrapper)`.

---

### Task 2 : `rescueops-common`, partie sécurité et identité

**Fichiers :**
- Créer : `rescueops-common/pom.xml` (jar, dépendances : `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-webmvc` en `optional`, `spring-boot-starter-amqp` en `optional`, `springdoc-openapi-starter-webmvc-ui` en `optional`, Lombok, et en test `spring-boot-starter-test` et `spring-security-test`)
- Créer dans `rescueops-common/src/main/java/tn/esprit/rescueops/common/security/` : `Role.java`, `KeycloakRoleConverter.java`, `CurrentUser.java`, `RescueOpsSecurityProperties.java`, `RescueOpsSecurityAutoConfiguration.java`
- Créer : `rescueops-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Tests : `rescueops-common/src/test/java/tn/esprit/rescueops/common/security/KeycloakRoleConverterTest.java`, `RescueOpsSecurityAutoConfigurationTest.java`

**Interfaces :**
- Produit :
  - `enum Role { CITOYEN, OPERATEUR, GESTIONNAIRE_RESSOURCES, AMBULANCIER, COORDINATEUR_HOSPITALIER, FORMATEUR, APPRENANT, ADMIN; String authority() /* "ROLE_"+name() */ }`
  - `class KeycloakRoleConverter implements Converter<Jwt, AbstractAuthenticationToken>` : lit `realm_access.roles`, garde **uniquement** les valeurs présentes dans `Role` et produit `JwtAuthenticationToken(jwt, authorities, jwt.getSubject())`.
  - `record CurrentUser(String keycloakId, String email, String givenName, String familyName, Set<Role> roles)`, avec `static CurrentUser from(Jwt jwt)`, `boolean has(Role r)` et `static Optional<CurrentUser> current()` (lit le `SecurityContextHolder`).
  - `@ConfigurationProperties("rescueops.security") record RescueOpsSecurityProperties(String issuer, String jwkSetUri, List<String> publicPaths)`. Valeurs par défaut :
    - issuer `http://localhost:8180/realms/rescueops` ;
    - jwkSetUri `http://localhost:8180/realms/rescueops/protocol/openid-connect/certs` ;
    - publicPaths `["/actuator/health/**","/actuator/info","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html"]`.
  - L'auto-configuration (`@ConditionalOnWebApplication(type=SERVLET)`) fournit :
    - un bean `JwtDecoder` (`@ConditionalOnMissingBean`) : `NimbusJwtDecoder.withJwkSetUri(jwkSetUri)` avec `JwtValidators.createDefaultWithIssuer(issuer)` ;
    - un bean `SecurityFilterChain` (`@ConditionalOnMissingBean(SecurityFilterChain.class)`) : stateless, CSRF désactivé, publicPaths en `permitAll`, le reste `authenticated`, `oauth2ResourceServer(jwt(converter))` ;
    - `@EnableMethodSecurity`.
    - Les réponses 401 et 403 sont au format ProblemDetail (`AuthenticationEntryPoint` et `AccessDeniedHandler` qui écrivent `application/problem+json`).

- [ ] **Étape 1 : écrire les tests qui doivent échouer.**

```java
@Test void rolesKeycloakDeviennentAutorites() {
  Jwt jwt = jwt(Map.of("realm_access", Map.of("roles", List.of("ADMIN","COORDINATEUR_HOSPITALIER","offline_access","uma_authorization"))));
  var auth = new KeycloakRoleConverter().convert(jwt);
  assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
      .containsExactlyInAnyOrder("ROLE_ADMIN","ROLE_COORDINATEUR_HOSPITALIER");
  assertThat(auth.getName()).isEqualTo(jwt.getSubject());
}
@Test void sansRealmAccessAucuneAutorite() { /* claims vides → authorities vide, aucune exception */ }
@Test void currentUserLitLesClaims() { /* sub, email, given_name, family_name, rôles → CurrentUser ; has(Role.ADMIN) == true */ }
```

`RescueOpsSecurityAutoConfigurationTest` utilise une petite application de test avec `@RestController` (`GET /secure` → 200, `GET /admin` avec `@PreAuthorize("hasRole('ADMIN')")`) et MockMvc :
- `/secure` sans token → **401**, `Content-Type: application/problem+json`, `status=401` ;
- `/secure` avec `jwt()` → **200** ;
- `/admin` avec `jwt().authorities(ROLE_CITOYEN)` → **403** au format ProblemDetail ;
- `/actuator/health` sans token → **200**.

- [ ] **Étape 2 : vérifier l'échec.** Lancer `./mvnw -pl rescueops-common test`. Résultat attendu : échec de compilation (les classes n'existent pas).
- [ ] **Étape 3 :** implémenter les classes de l'interface ci-dessus.
- [ ] **Étape 4 : vérifier le succès.** Lancer `./mvnw -pl rescueops-common test`. Résultat attendu : `BUILD SUCCESS`, 0 échec.
- [ ] **Étape 5 :** commit `feat(common): sécurité JWT Keycloak et rôles partagés`.

---

### Task 3 : `rescueops-common`, partie erreurs, corrélation et événements

**Fichiers :**
- Créer dans `common/error/` : `ApiException.java` (abstraite, avec `HttpStatus status()` et `String code()`), `NotFoundException`, `ConflictException`, `BusinessRuleException` (422), `ForbiddenOperationException` (403), `GlobalExceptionHandler.java`
- Créer dans `common/web/` : `CorrelationIdFilter.java`
- Créer dans `common/event/` : `RescueOpsEvent.java`, `RescueOpsAmqpAutoConfiguration.java`
- Créer dans `common/openapi/` : `OpenApiAutoConfiguration.java` (schéma de sécurité bearer-jwt, titre lu depuis `spring.application.name`)
- Modifier : le fichier `AutoConfiguration.imports`
- Tests : `GlobalExceptionHandlerTest.java`, `CorrelationIdFilterTest.java`, `RescueOpsEventTest.java`

**Interfaces :**
- Consomme : l'auto-configuration de sécurité de la Tâche 2.
- Produit :
  - `GlobalExceptionHandler` (`@RestControllerAdvice`, `@ConditionalOnMissingBean`) convertit les exceptions en `ProblemDetail` :
    - `ApiException` → son statut, avec la propriété `code` ;
    - `MethodArgumentNotValidException` et `ConstraintViolationException` → 400, avec `errors=[{field,message}]` ;
    - `DataIntegrityViolationException` → 409, `code=CONFLIT_DONNEES` et un message générique (**aucun détail SQL**) ;
    - toute autre `Exception` → 500 avec un message générique, la trace complète partant dans les logs.
    - Toutes les réponses portent `correlationId` et `instance` (le chemin de la requête).
  - `CorrelationIdFilter` (`OncePerRequestFilter`, ordre `HIGHEST_PRECEDENCE`) :
    - il lit `X-Correlation-Id` s'il correspond à `^[A-Za-z0-9-]{8,64}$`, sinon il génère un UUID ;
    - il le place dans le MDC `correlationId` et le renvoie dans l'en-tête de réponse ;
    - il nettoie le MDC à la fin de la requête.
  - `RequestInterceptor` Feign (si Feign est sur le classpath) qui propage `X-Correlation-Id` et `Authorization`.
  - `record RescueOpsEvent<T>(UUID eventId, String eventType, int version, Instant occurredAt, String correlationId, T payload)`, avec `static <T> RescueOpsEvent<T> of(String eventType, T payload)` (version=1, correlationId lu dans le MDC).
  - `RescueOpsAmqpAutoConfiguration` (`@ConditionalOnClass(RabbitTemplate.class)`) :
    - déclare `TopicExchange("rescueops.events")` et `DirectExchange("rescueops.events.dlx")` ;
    - fournit un `JacksonJsonMessageConverter` (Jackson 3) ;
    - fixe `RabbitTemplate.exchange = rescueops.events` ;
    - ajoute un `MessagePostProcessor` qui copie le `correlationId` dans l'en-tête `X-Correlation-Id`.
  - Le motif de log par défaut de la bibliothèque inclut `[%X{correlationId:-}]` (fichier `rescueops-common/src/main/resources/rescueops-logging.properties`, importé via `spring.config.import` dans les services).

- [ ] **Étape 1 : écrire les tests qui doivent échouer** (MockMvc sur un contrôleur de test qui lève chaque exception).
  - `NotFoundException("Hôpital introuvable")` → `status=404`, `code=RESSOURCE_INTROUVABLE`, `detail="Hôpital introuvable"`, `correlationId` non vide.
  - Un DTO `@Valid` avec un `@NotBlank nom` vide → 400, `errors[0].field="nom"`.
  - `DataIntegrityViolationException("Duplicate entry 'x@y' for key 'uk_email'")` → 409, et **le corps ne contient pas** `Duplicate entry` ni `uk_email`.
  - `RuntimeException("boom")` → 500, et le corps ne contient pas `boom`.
  - `X-Correlation-Id: abc12345-test` → le même en-tête est renvoyé et la valeur figure dans le corps d'erreur.
  - `X-Correlation-Id: <script>` → remplacé par un UUID.
  - `RescueOpsEvent.of("transfert.confirme", payload)` → `eventId` non nul, `version=1`, `occurredAt` ≤ maintenant. Un aller-retour JSON donne le même objet.
- [ ] **Étape 2 : vérifier l'échec.** Lancer `./mvnw -pl rescueops-common test`. Résultat attendu : les nouveaux tests échouent.
- [ ] **Étape 3 :** implémenter les classes de l'interface ci-dessus.
- [ ] **Étape 4 : vérifier le succès.** Lancer `./mvnw -pl rescueops-common install`. Résultat attendu : `BUILD SUCCESS` et le jar installé dans le dépôt local.
- [ ] **Étape 5 :** commit `feat(common): erreurs ProblemDetail, corrélation, événements RabbitMQ, OpenAPI`.

---

### Task 4 : infrastructure Docker (MySQL, Keycloak avec realm, RabbitMQ)

**Fichiers :**
- Créer :
  - `infrastructure/mysql/init/01-databases.sh` (script shell, pour lire les mots de passe depuis l'environnement) ;
  - `infrastructure/keycloak/realm-rescueops.json` ;
  - `infrastructure/rabbitmq/definitions.json` et `infrastructure/rabbitmq/rabbitmq.conf` ;
  - `docker-compose.infra.yml` (services `mysql`, `keycloak`, `rabbitmq` à cette étape) ;
  - `scripts/get-token.sh` et `scripts/get-token.ps1`.

**Interfaces :**
- Produit pour les plans suivants :
  - les bases `user_db`, `hospital_db`, `emergency_db`, `resource_db`, `dispatch_db`, `intervention_db`, `training_db` ;
  - les comptes `user_svc`, `hospital_svc`, `emergency_svc`, `resource_svc`, `dispatch_svc`, `intervention_svc`, `training_svc`, chacun avec `ALL PRIVILEGES` sur sa base uniquement ;
  - les clients Keycloak `rescueops-web` (public, PKCE S256, redirect `http://localhost:4200/*`, web origin `http://localhost:4200`, direct grant **désactivé**) et `rescueops-dev-cli` (public, **direct access grants activés**, réservé aux tests et à Postman, avec un commentaire « développement uniquement ») ;
  - 8 rôles de realm, rôle par défaut `CITOYEN`, `registrationAllowed=true`, `loginWithEmailAllowed=true` ;
  - les utilisateurs de démonstration `citoyen@rescueops.test`, `operateur@…`, `gestionnaire@…`, `ambulancier@…`, `coordinateur@…`, `formateur@…`, `apprenant@…`, `admin@…`, chacun avec son rôle, `firstName` et `lastName` renseignés, mot de passe `Demo1234!` (documenté comme non sensible) ;
  - `scripts/get-token.sh <email> [password]`, qui affiche l'access token (grant password sur `rescueops-dev-cli`).
- Détails de compose :
  - `mysql:8.4.13`, volume `mysql-data`, healthcheck `mysqladmin ping`, port 3306 ;
  - `quay.io/keycloak/keycloak:26.8.2`, commande `start-dev --import-realm`, `KC_HOSTNAME=http://localhost:8180`, `KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`, `KC_HEALTH_ENABLED=true`, base Keycloak sur le volume `keycloak-data` (H2 en développement), port `8180:8080`, healthcheck sur le port de gestion 9000 `/health/ready` ;
  - `rabbitmq:4.3.8-management`, `definitions.json` chargé (exchanges `rescueops.events` topic durable et `rescueops.events.dlx`), ports 5672 et 15672, healthcheck `rabbitmq-diagnostics -q ping` ;
  - réseau `rescueops-net`. Tous les secrets viennent de `.env`.

- [ ] **Étape 1 :** écrire les fichiers. Copier `.env.example` vers `.env` en local (ce fichier n'est pas commité).
- [ ] **Étape 2 : démarrer.** Lancer `docker compose -f docker-compose.infra.yml up -d`, puis `docker compose -f docker-compose.infra.yml ps`. Résultat attendu : les 3 services `healthy` en moins de 120 s.
- [ ] **Étape 3 : vérifier l'isolation MySQL.**
  - `docker exec … mysql -uuser_svc -p$USER_DB_PASSWORD -e "SHOW DATABASES"` → seulement `user_db` (avec `information_schema` et `performance_schema`).
  - `… -e "USE hospital_db"` → `ERROR 1044 (42000): Access denied`.
- [ ] **Étape 4 : vérifier le token.**
  - `TOKEN=$(scripts/get-token.sh coordinateur@rescueops.test)`, puis décoder la partie charge utile (base64).
  - Résultat attendu : `"iss":"http://localhost:8180/realms/rescueops"`, `realm_access.roles` contient `COORDINATEUR_HOSPITALIER` et `CITOYEN` (rôle par défaut), et `given_name` et `email` sont présents.
- [ ] **Étape 5 : vérifier RabbitMQ.** Lancer `curl -u $RABBITMQ_USER:$RABBITMQ_PASSWORD http://localhost:15672/api/exchanges/%2F/rescueops.events`. Résultat attendu : `"type":"topic"`, `"durable":true`.
- [ ] **Étape 6 :** commit `feat(infra): MySQL isolé par service, realm Keycloak rescueops, RabbitMQ`.

---

### Task 5 : Discovery Server et API Gateway

**Fichiers :**
- Créer : `infrastructure/discovery-server/` (`pom.xml` avec parent rescueops, `DiscoveryServerApplication`, `application.yml` repris du projet existant `discovery-server` : port `${SERVER_PORT:8761}`, `register-with-eureka:false`, `fetch-registry:false`, auto-préservation désactivée, Actuator health et info), son `Dockerfile` et son test de contexte.
- Créer dans `infrastructure/api-gateway/` :
  - `pom.xml` : `spring-cloud-starter-gateway-server-webflux`, `spring-cloud-starter-netflix-eureka-client`, `spring-boot-starter-oauth2-resource-server`, `spring-boot-starter-actuator`, `springdoc-openapi-starter-webflux-ui`. **Pas de `rescueops-common`**, qui est servlet.
  - `GatewayApplication`, `security/GatewaySecurityConfig.java`, `security/ReactiveKeycloakRoleConverter.java`, `filter/StripIdentityHeadersFilter.java`, `filter/CorrelationIdGlobalFilter.java`, `error/GatewayProblemHandler.java`.
  - `application.yml`, `application-local.yml`, `application-docker.yml`, `Dockerfile`.
- Tests : `GatewaySecurityTest.java`, `GatewayRoutesTest.java`, `StripIdentityHeadersFilterTest.java`, `CorsTest.java`

**Interfaces :**
- Consomme : le realm et l'émetteur de la Tâche 4.
- Produit :
  - **Les routes** (préfixe conservé, `lb://`) :

    | Chemins | Destination |
    |---|---|
    | `/api/users/**` | `ms-user` |
    | `/api/hospitals/**`, `/api/transfers/**` | `ms-hospital` |
    | `/api/emergencies/**` | `ms-emergency` |
    | `/api/resources/**` | `ms-resource` |
    | `/api/dispatch/**` | `ms-dispatch` |
    | `/api/interventions/**` | `ms-intervention` |
    | `/api/trainings/**` | `ms-training` |

  - **Les routes de documentation** : `/docs/<svc>/v3/api-docs` → `lb://<svc>` avec `RewritePath=/docs/<svc>/(?<p>.*), /${p}`. Elles sont publiques. Swagger UI est sur `/swagger-ui.html`, avec `springdoc.swagger-ui.urls` qui liste les 7 services.
  - **La sécurité** : `/actuator/health/**`, `/docs/**`, `/swagger-ui/**`, `/v3/api-docs/**` et `OPTIONS /**` sont publics. Tout le reste exige un JWT valide (même règle émetteur et JWKS que dans common, propriétés `rescueops.security.*`).
  - **`StripIdentityHeadersFilter`** (GlobalFilter, ordre -100) supprime `X-User-Id`, `X-User-Roles`, `X-Forwarded-User` et `X-Auth-Request-*` de la requête entrante.
  - **`CorrelationIdGlobalFilter`** : même règle que la Tâche 3, en version réactive.
  - **CORS** : origine autorisée `http://localhost:4200` (liste dans `rescueops.cors.allowed-origins`), méthodes GET, POST, PUT, PATCH, DELETE, OPTIONS, en-têtes `Authorization`, `Content-Type` et `X-Correlation-Id`, en-tête exposé `X-Correlation-Id`.
  - **Erreurs** : service absent d'Eureka ou injoignable → **503** `application/problem+json` (`code=SERVICE_INDISPONIBLE`, `detail` indique le service). Délai dépassé (`connect-timeout` 3 s, `response-timeout` 10 s) → 504, au même format.
  - **Profils** :
    - `local` : `eureka.client.service-url.defaultZone=http://localhost:8761/eureka/`, JWKS `http://localhost:8180/...` ;
    - `docker` : `http://discovery-server:8761/eureka/`, JWKS `http://keycloak:8080/realms/rescueops/protocol/openid-connect/certs`.
    - Dans les deux, `rescueops.security.issuer=http://localhost:8180/realms/rescueops`.

- [ ] **Étape 1 : discovery-server.** Écrire le module et son test `contextLoads`. Lancer `./mvnw -pl infrastructure/discovery-server test`. Résultat attendu : `BUILD SUCCESS`.
- [ ] **Étape 2 : écrire les tests de la Gateway qui doivent échouer** (WebTestClient, `@SpringBootTest(webEnvironment=RANDOM_PORT)`, Eureka désactivé, `JwtDecoder` remplacé par un mock).
  - `GET /api/hospitals/x` sans token → 401.
  - `GET /api/hospitals/x` avec `mockJwt()` et aucune instance → **503**, `Content-Type: application/problem+json`, `code=SERVICE_INDISPONIBLE`.
  - `GatewayRoutesTest` : le `RouteLocator` contient exactement les 7 routes métier + 7 routes de documentation, et chaque prédicat correspond au chemin et à l'URI listés ci-dessus.
  - `StripIdentityHeadersFilterTest` : un échange avec `X-User-Id: admin` → la requête transmise à la chaîne ne contient plus `X-User-Id`.
  - `CorsTest` :
    - `OPTIONS /api/users/me` avec `Origin: http://localhost:4200` → `Access-Control-Allow-Origin: http://localhost:4200` ;
    - avec `Origin: http://evil.test` → pas d'en-tête `Allow-Origin`, statut 403.
  - `GET /actuator/health` → 200 sans token.
- [ ] **Étape 3 : vérifier l'échec.** Lancer `./mvnw -pl infrastructure/api-gateway test`. Résultat attendu : échec.
- [ ] **Étape 4 :** implémenter la Gateway selon l'interface ci-dessus.
- [ ] **Étape 5 : vérifier le succès.** Lancer `./mvnw -pl infrastructure/api-gateway -am test`. Résultat attendu : `BUILD SUCCESS`.
- [ ] **Étape 6 :** Dockerfiles multi-étapes pour les deux modules. Le build se fait **depuis la racine** du dépôt (contexte `.`, avec l'argument `MODULE`), pour que le POM parent soit disponible. Image d'exécution `eclipse-temurin:21-jre`, utilisateur non root, `HEALTHCHECK` sur `/actuator/health`.
- [ ] **Étape 7 :** commit `feat(infra): discovery-server Eureka et api-gateway (routes, JWT, CORS, erreurs)`.

---

### Task 6 : socle complet en une commande et vérification de bout en bout

**Fichiers :**
- Modifier : `docker-compose.infra.yml` pour ajouter `discovery-server` et `api-gateway`.
  - Builds depuis la racine.
  - `SPRING_PROFILES_ACTIVE=docker`.
  - `depends_on` avec `condition: service_healthy` (la Gateway dépend de discovery-server et de keycloak).
  - `extra_hosts: ["host.docker.internal:host-gateway"]` sur la Gateway.
- Créer : `scripts/smoke-infra.sh` et son équivalent `scripts/smoke-infra.ps1`
- Créer : `tools/echo-service/` (module Maven **hors `<modules>`**, utilisé seulement pour le smoke test). Il dépend de `rescueops-common` et du client Eureka. Son nom Eureka est `ms-hospital`, son port 8085, et il expose `GET /api/hospitals/echo`, qui renvoie `{sub, roles, headers}`.
- Modifier : `README.md` (prérequis, `cp .env.example .env`, commande unique, URLs, comptes de démo, dépannage).

**Interfaces :**
- Consomme : toutes les tâches précédentes.
- Produit pour les plans 2 à 4 : la recette du **profil `local`** d'un service lancé sur l'hôte. Elle est **documentée dans le README** et vérifiée ici :

```yaml
# application-local.yml d'un service (lancé depuis IntelliJ)
eureka:
  client.service-url.defaultZone: http://localhost:8761/eureka/
  instance:
    hostname: host.docker.internal
    prefer-ip-address: false
rescueops.security.jwk-set-uri: http://localhost:8180/realms/rescueops/protocol/openid-connect/certs
```

- [ ] **Étape 1 :** compléter le compose et lancer `docker compose -f docker-compose.infra.yml up -d --build`. Résultat attendu : 5 services `healthy`.
- [ ] **Étape 2 :** lancer l'echo-service sur l'hôte avec `./mvnw -f tools/echo-service spring-boot:run -Dspring-boot.run.profiles=local`. Résultat attendu : il apparaît dans http://localhost:8761 sous `MS-HOSPITAL`, au statut UP.
- [ ] **Étape 3 :** écrire et lancer `scripts/smoke-infra.sh`, qui vérifie dans l'ordre :
  1. `curl :8080/actuator/health` → `UP` ;
  2. `GET :8080/api/hospitals/echo` sans token → 401 ;
  3. avec le token `coordinateur@rescueops.test` → 200, `sub` non vide, `roles` contient `ROLE_COORDINATEUR_HOSPITALIER` ;
  4. avec le token et l'en-tête `X-User-Id: pirate` → 200, et `headers` **ne contient pas** `x-user-id` ;
  5. `GET :8080/api/emergencies/x` avec token → 503 ProblemDetail (aucun service ms-emergency) ;
  6. réponse portant `X-Correlation-Id`.
  
  Résultat attendu : `SMOKE OK (6/6)`.
- [ ] **Étape 4 : vérifier la persistance.**
  - `docker compose -f docker-compose.infra.yml down`, puis `up -d`.
  - Reprendre un token admin et créer un utilisateur de test dans l'admin Keycloak avant le `down`. Après le `up`, il existe toujours, et le realm n'a pas été réimporté par-dessus.
  - Relancer le smoke test. Résultat attendu : `SMOKE OK (6/6)`.
- [ ] **Étape 5 :** arrêter l'echo-service. Commit `feat(infra): socle complet en une commande + smoke test de bout en bout`.

---

## Après ce plan
- **Plan 2 (MS User)** est rédigé et exécuté ensuite, sur ce socle (la route `/api/users/**` est déjà en place).
- Rien n'est poussé vers `eminealoui/rescueops` sans l'accord du porteur.
