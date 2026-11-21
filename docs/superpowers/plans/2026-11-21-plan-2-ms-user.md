# Plan 2 : MS User (profils métier liés à Keycloak)

> **Pour les agents qui exécutent ce plan :** SOUS-SKILL REQUIS : superpowers:executing-plans (choisi par le porteur : exécution « Native »). Les étapes utilisent la syntaxe à cocher (`- [ ]`).

**But :** livrer `ms-user` (port 8087), le service partagé par toute l'équipe, qui gère les **profils métier** liés à un compte Keycloak.

**Architecture :**
- Spring Boot servlet, avec `rescueops-common` (sécurité, erreurs) ;
- MySQL `user_db`, schéma géré par Flyway ;
- le profil est **créé automatiquement au premier `GET /api/users/me`**, à partir des informations du JWT ;
- les rôles **ne sont jamais** stockés : ils viennent du token.

**Stack :**
- Spring Boot 4.2.0, Spring Data JPA, Flyway (`flyway-mysql`), MapStruct 1.6.3 ;
- tests : Testcontainers 2.0.5 (`testcontainers-mysql`, `spring-boot-testcontainers`).

**Spec :** `docs/superpowers/specs/2026-11-21-rescueops-socle-hospital-design.md` §7, et §3 pour l'identifiant `sub`.

## Contraintes globales
- **Service** : module `services/ms-user`, nom Eureka `ms-user`, port **8087**, package `tn.esprit.rescueops.user`, préfixe `/api/users`. La route Gateway existe déjà.
- **Base** : `user_db`, compte `user_svc` (mot de passe `USER_DB_PASSWORD`), MySQL sur le port hôte **3307**, `ddl-auto: validate`.
- **Identifiant public** : le **`keycloakId`** (le `sub` du JWT) dans toutes les routes. L'`id` interne ne sort jamais du service.
- **Aucun mot de passe et aucun rôle** dans `user_db`. Keycloak est la seule autorité pour les rôles.
- **Erreurs** au format ProblemDetail de `rescueops-common` (`NotFoundException`, `ConflictException`, `ForbiddenOperationException`).
- **Même structure que les squelettes de l'équipe** : profils `local` et `docker`, `/ping` n'est pas nécessaire.

## Points de vigilance
1. **Deux premiers `GET /me` simultanés** (onglets ouverts en même temps) : un seul profil doit être créé, et les deux réponses sont 200. Couvert par le test de la Tâche 3.
2. **Token sans `given_name` ou `family_name`** (compte créé sans nom) : le profil est créé avec des valeurs vides, sans erreur 500. Couvert par le test de la Tâche 1.
3. **`PUT /me` avec un `keycloakId`, un `email`, `active` ou `roles` dans le corps** : ces champs sont ignorés et ne peuvent pas être modifiés. Couvert en Tâche 2.
4. **Pagination abusive** (`size=10000`, `sort=motDePasse`) : la taille est plafonnée à 100, et un tri inconnu renvoie 400 (pas 500). Couvert en Tâche 2.
5. **Email changé dans Keycloak** : il est resynchronisé depuis le token à chaque `GET /me`. Couvert en Tâche 1.

---

### Task 1 : profil, migration et création au premier accès

**Fichiers :**
- Créer : `services/ms-user/pom.xml`, en reprenant le modèle d'un squelette (`services/ms-emergency/pom.xml`), sans RabbitMQ ni Feign, et en ajoutant en test `spring-boot-testcontainers`, `testcontainers-mysql` et `testcontainers-junit-jupiter`. Ajouter `<module>services/ms-user</module>` au POM parent.
- Créer : `UserServiceApplication.java`, `application.yml`, `application-local.yml` et `application-docker.yml` (modèle des squelettes, avec le port 8087 et `user_db`, `user_svc`, `dev_user`).
- Créer : `src/main/resources/db/migration/V1__creation_user_profile.sql`.
- Créer : `domain/UserProfile.java`, `repository/UserProfileRepository.java`, `service/UserProfileService.java`, `dto/UserProfileResponse.java`, `mapper/UserProfileMapper.java`.
- Test : `service/UserProfileServiceTest.java` (Mockito).

**Interfaces :**
- **Table `user_profile`** :
  - `id BINARY(16) PK` ;
  - `keycloak_id VARCHAR(36) NOT NULL UNIQUE (uk_user_profile_keycloak_id)` ;
  - `email VARCHAR(254) UNIQUE (uk_user_profile_email)` ;
  - `first_name VARCHAR(100)`, `last_name VARCHAR(100)` ;
  - `phone VARCHAR(20)` ;
  - `address VARCHAR(255)` ;
  - `organisation VARCHAR(150)` ;
  - `active BOOLEAN NOT NULL DEFAULT TRUE` ;
  - `created_at DATETIME(6) NOT NULL`, `updated_at DATETIME(6) NOT NULL` ;
  - `version BIGINT NOT NULL` ;
  - index sur `last_name`.
- **Entité** : `UserProfile`, avec les mêmes champs en camelCase, `@Version`, et `@PrePersist` / `@PreUpdate` qui remplissent les dates.
- **Repository** : `UserProfileRepository extends JpaRepository<UserProfile, UUID>`, avec `Optional<UserProfile> findByKeycloakId(String keycloakId)`.
- **DTO** : `record UserProfileResponse(String keycloakId, String email, String firstName, String lastName, String phone, String address, String organisation, boolean active, Instant createdAt, Instant updatedAt)`.
- **Service** : `UserProfileService.getOrCreateCurrent(CurrentUser user) : UserProfileResponse`.
  - Il cherche le profil par `keycloakId`. S'il est absent, il le crée depuis le JWT (`givenName`, `familyName`, `email`).
  - Si le profil existe et que l'email du token diffère, il met l'email à jour.
  - En cas de `DataIntegrityViolationException` à la création (course entre deux requêtes), il **relit** par `keycloakId` et renvoie le profil existant. La création se fait donc dans sa propre transaction (`TransactionTemplate` avec `PROPAGATION_REQUIRES_NEW`), puis on relit.

- [ ] **Étape 1 : écrire les tests qui échouent** (`UserProfileServiceTest`, repository simulé) :
  - `premierAccesCreeLeProfilDepuisLeToken` : aucun profil, puis `save` appelé avec `keycloakId="sub-1"`, `email="a@rescueops.test"`, `firstName="Amine"`, `active=true`.
  - `secondAccesRenvoieLeProfilSansRecreer` : profil existant, donc `save` n'est jamais appelé.
  - `emailChangeDansKeycloakEstResynchronise` : profil avec `old@x`, token avec `new@x`, la réponse contient `new@x`.
  - `tokenSansNomCreeUnProfilSansErreur` : `givenName` et `familyName` nuls, le profil est créé avec des noms nuls.
  - `courseDeCreationRelitLeProfilExistant` : le premier `findByKeycloakId` est vide, `save` lève `DataIntegrityViolationException`, le second `findByKeycloakId` renvoie un profil. Pas d'exception, et la réponse correspond à ce profil.
- [ ] **Étape 2 : vérifier l'échec.** Lancer `./mvnw -pl services/ms-user -am test -Dtest=UserProfileServiceTest -Dsurefire.failIfNoSpecifiedTests=false`. Résultat attendu : échec de compilation.
- [ ] **Étape 3 : implémenter** les interfaces ci-dessus. Le mapper MapStruct va de `UserProfile` vers `UserProfileResponse`.
- [ ] **Étape 4 : vérifier le succès.** Même commande. Résultat attendu : 5 tests, 0 échec.
- [ ] **Étape 5 :** commit `feat(user): profil UserProfile, migration Flyway et création au premier accès`.

---

### Task 2 : API REST et règles d'accès

**Fichiers :**
- Créer : `web/UserProfileController.java`, `dto/UpdateMyProfileRequest.java`, `dto/UpdateStatusRequest.java`, `dto/UserSummaryDto.java`, `dto/PageResponse.java`.
- Modifier : `UserProfileService.java`, `UserProfileRepository.java`.
- Test : `web/UserProfileControllerTest.java` (`@WebMvcTest` impossible avec l'auto-configuration du common : utiliser `@SpringBootTest` avec exclusion de DataSource, Hibernate et Flyway, comme les squelettes, et `@MockitoBean UserProfileService`).

**Interfaces :**
- **`record UpdateMyProfileRequest`**, avec `@JsonIgnoreProperties(ignoreUnknown = true)` :
  - `@NotBlank @Size(max=100) String firstName` ;
  - `@NotBlank @Size(max=100) String lastName` ;
  - `@Pattern(regexp="^\\+?[1-9][0-9]{7,14}$") String phone` (facultatif) ;
  - `@Size(max=255) String address` (facultatif).
- **`record UpdateStatusRequest(@NotNull Boolean active)`.**
- **`record UserSummaryDto(String keycloakId, String displayName, String organisation)`** : `displayName` vaut « Prénom Nom », ou l'email si le nom est vide.
- **`record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages)`.**
- **Service :**
  - `updateCurrent(CurrentUser, UpdateMyProfileRequest)` : si le profil est inactif, lève `ForbiddenOperationException("Profil désactivé : modification impossible")` ;
  - `findByKeycloakId(String)` : lève `NotFoundException("Profil introuvable")` si absent ;
  - `summary(String)` ;
  - `search(String query, Boolean active, int page, int size, String sort)` ;
  - `setActive(String keycloakId, boolean active)`.
- **Repository** : `@Query` de recherche, qui ignore la casse et cherche `query` dans `firstName`, `lastName` ou `email`, avec filtre `active` facultatif, et renvoie une `Page<UserProfile>`.

**Routes :**

| Route | Accès | Comportement |
|---|---|---|
| `GET /api/users/me` | authentifié | renvoie `getOrCreateCurrent` |
| `PUT /api/users/me` | authentifié | renvoie le profil mis à jour |
| `GET /api/users/{keycloakId}` | `hasRole('ADMIN')` | profil complet |
| `GET /api/users?query&active&page=0&size=20&sort=lastName,asc` | `hasRole('ADMIN')` | **size plafonnée à 100** ; tri autorisé seulement sur `lastName`, `firstName`, `email`, `createdAt`, sinon **400** `TRI_INVALIDE` (voir la décision ci-dessous) |
| `PATCH /api/users/{keycloakId}/status` | `hasRole('ADMIN')` | renvoie le profil |
| `GET /api/users/{keycloakId}/summary` | authentifié | renvoie `UserSummaryDto` |

**Décision :** un tri inconnu renvoie **400** `code=TRI_INVALIDE` (pas 422 : c'est un paramètre mal formé). On ajoute à `rescueops-common` l'exception `BadRequestException` (400, code libre). C'est une extension du common, dont la nécessité est démontrée par ce test.

- [ ] **Étape 1 : écrire les tests qui échouent** (`UserProfileControllerTest`) :
  - `meSansToken401` ;
  - `meAvecTokenRenvoieLeProfil` : `$.keycloakId`, et `$.id` **absent** ;
  - `putMeIgnoreLesChampsInterdits` : le corps contient `{"firstName":"A","lastName":"B","keycloakId":"pirate","active":false,"roles":["ADMIN"]}`, et le service reçoit une requête avec seulement A et B ;
  - `putMePrenomVide400` : `$.errors[0].field=firstName` ;
  - `putMeTelephoneInvalide400` ;
  - `listeParCitoyen403` ;
  - `listeParAdminTailleBornee` : `size=10000`, et le service est appelé avec `size=100` ;
  - `listeTriInconnu400` : `sort=motDePasse,asc` donne 400 avec `code=TRI_INVALIDE` ;
  - `statutParAdmin200` ;
  - `profilInconnu404` ;
  - `summaryAccessibleATousLesAuthentifies`.
- [ ] **Étape 2 :** lancer `./mvnw -pl services/ms-user -am test -Dtest=UserProfileControllerTest -Dsurefire.failIfNoSpecifiedTests=false`. Résultat attendu : échec.
- [ ] **Étape 3 :** implémenter (contrôleur, DTO, méthodes du service et du repository, `BadRequestException` dans common avec un test dans `GlobalExceptionHandlerTest` : `badRequestDonne400AvecSonCode`).
- [ ] **Étape 4 :** même commande, plus `./mvnw -pl rescueops-common test`. Résultat attendu : tout est vert.
- [ ] **Étape 5 :** commit `feat(user): API /me, administration et recherche paginée`.

---

### Task 3 : intégration MySQL réelle (Testcontainers)

**Fichiers :**
- Test : `services/ms-user/src/test/java/tn/esprit/rescueops/user/UserProfileIntegrationTest.java`.

**Interfaces :**
- Consomme : les Tâches 1 et 2.
- Le test lance `@Testcontainers` avec `MySQLContainer("mysql:8.4.13")` en `@ServiceConnection`, `@SpringBootTest`, Eureka désactivé et santé RabbitMQ désactivée, puis appelle le **vrai** MockMvc avec `jwt()`.

- [ ] **Étape 1 : écrire les tests :**
  - `flywayEtValidationHibernateDemarrent` : le contexte démarre, ce qui prouve que la migration correspond aux entités ;
  - `deuxPremiersAccesSimultanesCreentUnSeulProfil` : 10 threads appellent `GET /me` avec le même `sub`, ils obtiennent tous 200, et `count()` vaut 1 ;
  - `rechercheParNomInsensibleALaCasse` : 3 profils, `query=ben` en minuscules trouve « Ben Salah » ;
  - `emailDejaPrisParUnAutreProfil409` : création de `sub-A` avec `x@y`, puis `GET /me` de `sub-B` avec `x@y` donne 409 `CONFLIT_DONNEES`.
- [ ] **Étape 2 :** lancer `./mvnw -pl services/ms-user -am test` (Docker doit tourner). Résultat attendu : tous les tests verts. Si un test échoue, corriger le code et non le test.
- [ ] **Étape 3 :** commit `test(user): intégration MySQL réelle (concurrence, recherche, unicité)`.

---

### Task 4 : branchement au socle et vérification de bout en bout

**Fichiers :**
- Modifier : `docker-compose.infra.yml`, pour ajouter `ms-user` :
  - build avec `docker/java-service.Dockerfile`, `MODULE=services/ms-user` et `PORT=8087` ;
  - `SPRING_PROFILES_ACTIVE=docker`, `DB_PASSWORD=${USER_DB_PASSWORD}` ;
  - `depends_on` : mysql, discovery-server et keycloak en état healthy ;
  - **pas de port publié** : on passe par la Gateway.
- Créer : `services/ms-user/README.md` (fiche du service, endpoints, comment l'appeler).
- Modifier : `README.md` racine, pour mentionner que ms-user est inclus dans le socle.

- [ ] **Étape 1 :** lancer `docker compose -f docker-compose.infra.yml up -d --build`. Résultat attendu : 6 services healthy, et `MS-USER` UP dans Eureka.
- [ ] **Étape 2 :** vérifier en PowerShell, avec de vrais tokens :
  - `GET /api/users/me` en coordinateur donne 200, avec `firstName = Meriem` ;
  - `PUT /me` (avec `Invoke-RestMethod`) change le téléphone ;
  - `GET /api/users` en citoyen donne 403, et en admin donne la liste ;
  - `PATCH /status` en admin donne `active=false`, puis le `PUT /me` du coordinateur donne 403.
  
  À la fin, remettre le statut à `active=true`.
- [ ] **Étape 3 :** vérifier dans phpMyAdmin (`user_svc`) : la table `user_profile` contient les profils, **sans aucune colonne de mot de passe ni de rôle**.
- [ ] **Étape 4 :** commit `feat(user): ms-user dans le socle partagé`.
