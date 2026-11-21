# Plan 3 : MS5 Hospital (coordination hospitalière)

> **Pour les agents qui exécutent ce plan :** SOUS-SKILL REQUIS : superpowers:executing-plans (exécution « Native »). Étapes à cocher (`- [ ]`).

**But :** livrer `ms-hospital` (port 8085), le module du porteur et le service de référence de l'équipe. Il couvre :
- les établissements, leurs services et leurs capacités (BF-13) ;
- la recherche d'établissements compatibles avec un score expliqué (BF-14) ;
- les demandes de transfert avec réservation temporaire, confirmation, refus et expiration (BF-15) ;
- **zéro double réservation** (BNF-05).

**Architecture :** même structure que `ms-user` et les squelettes (`domain / repository / dto / mapper / service / web / event`).
- La capacité ne se modifie **que** par des `UPDATE` conditionnels atomiques.
- Chaque transition de demande est **conditionnelle à son statut** (`UPDATE ... WHERE statut = :attendu`).
- Les événements partent **après commit**.

**Stack :**
- Spring Boot 4.2, JPA, Flyway, RabbitMQ (`rescueops-common`), MapStruct, `@Scheduled` ;
- tests : Testcontainers MySQL et RabbitMQ.

**Spec :** `docs/superpowers/specs/2026-11-21-rescueops-socle-hospital-design.md` §8 (modèle, machine à états, règles, API, événements, tests).

## Contraintes globales
- **Service** : module `services/ms-hospital`, nom Eureka `ms-hospital`, port **8085**, package `tn.esprit.rescueops.hospital`, base `hospital_db`, compte `hospital_svc`. Les routes Gateway `/api/hospitals/**` et `/api/transfers/**` existent déjà.
- **Rôles** :
  - écriture sur les établissements et services : `COORDINATEUR_HOSPITALIER`, `ADMIN` ;
  - création d'une demande : `AMBULANCIER`, `OPERATEUR` ;
  - réservation : `AMBULANCIER`, `OPERATEUR`, `COORDINATEUR_HOSPITALIER` ;
  - confirmation, refus, admission, sortie : `COORDINATEUR_HOSPITALIER`, `ADMIN` ;
  - annulation : le demandeur, `COORDINATEUR_HOSPITALIER`, `ADMIN` ;
  - lecture : tout utilisateur authentifié ;
  - `/api/transfers/feed` : rôles opérationnels uniquement (OPERATEUR, AMBULANCIER, COORDINATEUR_HOSPITALIER, GESTIONNAIRE_RESSOURCES, ADMIN).
- **Identifiants utilisateur** : le `sub` Keycloak (`CurrentUser`). **`missionId`** est un UUID externe, sans clé étrangère.
- **Spécialités** : `REANIMATION, TRAUMATOLOGIE, CARDIOLOGIE, PEDIATRIE, NEUROLOGIE, BRULES, URGENCES_GENERALES, MATERNITE`.
- **Gravité** : `FAIBLE, MOYENNE, ELEVEE, CRITIQUE`.
- **Statuts** : `DEMANDEE, PLACE_RETENUE, CONFIRMEE, ADMIS, SORTI, REFUSEE, EXPIREE, ANNULEE`.
- **Configuration** :
  - `rescueops.hospital.reservation-ttl` (15 min par défaut) ;
  - `rescueops.hospital.max-radius-km` (50) ;
  - `rescueops.hospital.expiry-check-interval` (30 s) ;
  - les poids du score, par gravité.
- **Erreurs** :
  - transition interdite : 409 `TRANSITION_INTERDITE` ;
  - plus de place : 409 `CAPACITE_EPUISEE` ;
  - capacité inférieure aux places prises : 422 `CAPACITE_INVALIDE` ;
  - ressource absente : 404.

## Points de vigilance
1. **50 réservations simultanées sur un service de 10 places** : exactement 10 succès, 40 réponses 409, et `places_disponibles = 0`. Test d'intégration de la Tâche 5.
2. **L'expiration et un refus arrivent en même temps** sur la même demande : la place n'est rendue qu'**une** fois. Tâche 5.
3. **Le même événement `mission.terminee` livré deux fois** : un seul effet. Tâche 4.
4. **Un service désactivé** n'est plus proposé et ne peut plus être réservé. Tâches 2 et 3.
5. **Coordonnées absentes ou invalides** (latitude hors de [-90, 90]) : 400. Une demande sans coordonnées ne casse pas le score : le sous-score distance vaut 0, et le mot « distance inconnue » apparaît dans la justification. Tâche 3.

---

### Task 1 : module, établissements, services, capacités

**Fichiers :**
- `services/ms-hospital/` : `pom.xml` (modèle `ms-user`, plus RabbitMQ, plus Testcontainers `testcontainers-rabbitmq`), `application*.yml` (modèle des squelettes, avec `hospital_db` et le port 8085), et le module ajouté au POM parent.
- `db/migration/V1__schema_hospital.sql`, avec les tables suivantes :
  - `hopital` (id, nom, adresse, ville, latitude `DECIMAL(9,6)`, longitude, telephone, actif, created_at, updated_at, version) ;
  - `service_hospitalier` (id, hopital_id **FK interne**, specialite, capacite_totale, places_disponibles, actif, version, `CHECK (places_disponibles BETWEEN 0 AND capacite_totale)`, `UNIQUE (hopital_id, specialite)`) ;
  - `demande_transfert` (id, mission_id, specialite_requise, gravite, latitude_origine, longitude_origine, service_id **FK interne** nullable, statut, expire_at, demandeur_user_id, traite_par_user_id, motif_refus, created_at, updated_at, version ; index `(statut, expire_at)`) ;
  - `historique_transfert` (id, demande_id, ancien_statut, nouveau_statut, acteur_user_id, horodatage, commentaire) ;
  - `historique_capacite` (id, service_id, ancienne_valeur, nouvelle_valeur, acteur_user_id, horodatage, motif) ;
  - `evenement_traite` (event_id PK, traite_le).
- `domain/` : `Hopital`, `ServiceHospitalier`, `Specialite`, plus `dto/`, `mapper/`, `repository/`, `service/EtablissementService`, `web/HopitalController`.

**Interfaces (produites) :**
- **`ServiceHospitalierRepository`** :
  - `int reserverPlace(UUID serviceId)` → `UPDATE ... SET places_disponibles = places_disponibles - 1 WHERE id = :id AND actif AND places_disponibles > 0` ;
  - `int rendrePlace(UUID serviceId)` → `+1 WHERE places_disponibles < capacite_totale`.
- **`EtablissementService`** :
  - CRUD hôpital ;
  - suppression **logique** (`actif = false`) ;
  - CRUD des services d'un hôpital ;
  - `ajusterCapacite(serviceId, capaciteTotale, placesDisponibles, motif)` : refuse avec 422 si `capaciteTotale < placesOccupées` (avec `placesOccupées = capaciteTotale actuelle − placesDisponibles actuelles`) ou si `placesDisponibles` sort de [0, capaciteTotale]. Chaque ajustement est écrit dans `historique_capacite` ;
  - `tableauDeBord(ville, specialite, statut)` : renvoie `{kpis: {placesLibres, reservationsActives, servicesSatures, tauxAcceptation24h}, services: [...]}`. Le statut d'un service vaut `SATURE` si `places_disponibles = 0`, `RESERVE` s'il a au moins une demande `PLACE_RETENUE`, et `DISPONIBLE` sinon.

**Routes :**

| Route | Accès |
|---|---|
| `GET/POST /api/hospitals`, `GET/PUT/DELETE /api/hospitals/{id}` | lecture : authentifié ; écriture : coordinateur, admin |
| `GET/POST /api/hospitals/{id}/services`, `PUT /api/hospitals/services/{serviceId}`, `PATCH /api/hospitals/services/{serviceId}/capacity` | idem |
| `GET /api/hospitals/capacities?ville=&specialite=&statut=` | authentifié |

- [ ] **Étape 1 : écrire les tests qui échouent.** Dans `EtablissementServiceTest` (Mockito) :
  - `ajusterCapaciteSousLesPlacesOccupees422` : 10 au total, 4 libres (donc 6 occupées), nouvelle capacité 5 → `BusinessRuleException` `CAPACITE_INVALIDE` ;
  - `ajusterCapaciteEcritLHistorique` ;
  - `supprimerHopitalEstLogique` ;
  - `statutSatureQuandAucunePlace`.
  
  Dans `HopitalControllerTest` : création par un citoyen → 403 ; latitude 95 → 400 ; lecture par un citoyen → 200.
- [ ] **Étape 2 :** RED, puis implémentation, puis GREEN avec `./mvnw -pl services/ms-hospital test`.
- [ ] **Étape 3 :** commit `feat(hospital): établissements, services et capacités`.

---

### Task 2 : demandes de transfert et machine à états

**Fichiers :**
- `domain/DemandeTransfert`, `StatutTransfert`, `Gravite`, `HistoriqueTransfert` ;
- `service/TransfertService` ;
- `web/TransfertController` ;
- les DTO.

**Interfaces :**
- **`DemandeTransfertRepository`** : `int changerStatut(UUID id, StatutTransfert attendu, StatutTransfert nouveau, String acteur, Instant now)` → `UPDATE ... WHERE id = :id AND statut = :attendu`.
- **`TransfertService`** (une transaction par opération). Chaque transition :
  1. vérifie le rôle et la règle ;
  2. appelle `changerStatut` ;
  3. si la requête ne touche aucune ligne, lève 409 `TRANSITION_INTERDITE` (message : « La demande n'est plus à l'état X ») ;
  4. écrit l'historique ;
  5. publie un événement Spring (`TransfertStatutChange`).

| Méthode | Transition | Effet sur la place |
|---|---|---|
| `creer(req)` | → `DEMANDEE` | — |
| `reserver(id, serviceId)` | `DEMANDEE` → `PLACE_RETENUE` | `reserverPlace` (0 ligne touchée → 409 `CAPACITE_EPUISEE`, et la transaction est annulée) ; `expire_at = now + ttl` |
| `confirmer(id)` | `PLACE_RETENUE` → `CONFIRMEE` | — |
| `refuser(id, motif)` | `PLACE_RETENUE` → `REFUSEE` | `rendrePlace` |
| `admettre(id)` | `CONFIRMEE` → `ADMIS` | — |
| `sortie(id)` | `ADMIS` → `SORTI` | `rendrePlace` |
| `annuler(id)` | depuis `DEMANDEE`, `PLACE_RETENUE` ou `CONFIRMEE` | `rendrePlace` si une place était prise |
| `expirer(id)` | `PLACE_RETENUE` (et `expire_at < now`) → `EXPIREE` | `rendrePlace` |

- **Annulation** : seuls le demandeur (`demandeurUserId == sub`), un coordinateur ou un admin peuvent annuler. Sinon, 403 `OPERATION_INTERDITE`.

**Routes :**
- `POST /api/transfers` ;
- `POST /api/transfers/{id}/reserve` (corps : `{serviceId}`), `/confirm`, `/refuse` (corps : `{motif}`), `/admit`, `/discharge`, `/cancel` ;
- `GET /api/transfers?statut=&page=&size=` ;
- `GET /api/transfers/{id}` (avec l'historique).

- [ ] **Étape 1 : écrire les tests qui échouent** (`TransfertServiceTest`, Mockito) :
  - `reserverSansPlace409CapaciteEpuisee` ;
  - `confirmerDepuisDemandee409` ;
  - `refuserRendLaPlace` ;
  - `sortieRendLaPlace` ;
  - `annulerConfirmeeRendLaPlace` ;
  - `annulerDemandeeNeRendRien` ;
  - `annulerParUnAutreAmbulancier403` ;
  - `chaqueTransitionEcritLHistorique`.
  
  Dans `TransfertControllerTest` : un citoyen qui crée → 403 ; un coordinateur qui confirme → 200 ; un ambulancier qui confirme → 403.
- [ ] **Étape 2 :** RED, implémentation, GREEN.
- [ ] **Étape 3 :** commit `feat(hospital): demandes de transfert, machine à états et réservation atomique`.

---

### Task 3 : établissements compatibles, expiration, flux du ticker

**Fichiers :**
- `service/CompatibiliteService` ;
- `service/ExpirationJob` ;
- la configuration `HospitalProperties` ;
- `GET /api/hospitals/compatible`, `GET /api/transfers/feed`.

**Interfaces :**
- **`CompatibiliteService.proposer(specialite, lat, lon, gravite)`** renvoie une liste triée de `CandidatDto(serviceId, hopitalId, hopitalNom, specialite, placesDisponibles, capaciteTotale, distanceKm, score, sousScores{distance, disponibilite, specialite}, justification)`.
  - **Candidats** : services actifs d'hôpitaux actifs, avec au moins une place, de la spécialité requise **ou** `URGENCES_GENERALES`, à moins de `max-radius-km`.
  - **Distance** : Haversine (rayon terrestre de 6371 km).
  - **Sous-scores** :
    - distance = `100 × (1 − d / rayon)` ;
    - disponibilité = `100 × libres / capacité` ;
    - spécialité = 100 si exacte, 40 si repli.
  - **Poids** (distance / disponibilité / spécialité) : `CRITIQUE` 0,6/0,1/0,3 ; `ELEVEE` 0,5/0,2/0,3 ; `MOYENNE` et `FAIBLE` 0,35/0,35/0,3.
  - **Score** : arrondi à l'entier.
  - **Justification** : par exemple « 4,1 km · 30 % de places libres · spécialité exacte ».
  - **Coordonnées absentes** : distance = 0, aucun filtre de rayon, et « distance inconnue » dans la justification.
- **`ExpirationJob`** (`@Scheduled(fixedDelayString = "${rescueops.hospital.expiry-check-interval:30s}")`) : il cherche les identifiants `PLACE_RETENUE` dont `expire_at < now` (100 au maximum), puis appelle `TransfertService.expirer(id)` pour chacun, dans **sa propre** transaction. Un 409 (déjà traité ailleurs) est ignoré.
- **`/api/transfers/feed?since=`** renvoie les 20 dernières transitions : `{id, statut, hopitalNom, horodatage}`, **sans données personnelles**.

- [ ] **Étape 1 : écrire les tests qui échouent** (`CompatibiliteServiceTest`, pur, sans base) :
  - `critiquePrivilegieLePlusProche` : A est à 3 km avec 1 place sur 20, B à 30 km avec 15 places sur 20, en `CRITIQUE` → A en premier ;
  - `faiblePrivilegieLaDisponibilite` : mêmes données en `FAIBLE` → B en premier ;
  - `replisUrgencesGeneralesMoinsBienNotes` ;
  - `horsRayonExclu` ;
  - `sansPlaceExclu` ;
  - `serviceInactifExclu` ;
  - `sansCoordonneesDistanceInconnue` ;
  - `justificationLisible` : contient « km » et « spécialité exacte ».
  
  Dans `ExpirationJobTest` : `expireLesReservationsEchues` et `ignoreUneDemandeDejaTraitee`.
- [ ] **Étape 2 :** RED, implémentation, GREEN.
- [ ] **Étape 3 :** commit `feat(hospital): établissements compatibles (score expliqué), expiration, flux`.

---

### Task 4 : événements RabbitMQ

**Fichiers :** `event/HospitalEventPublisher`, `event/MissionTermineeListener`, `event/MessagingConfig`, `domain/EvenementTraite`.

**Interfaces :**
- **Publication `@TransactionalEventListener(phase = AFTER_COMMIT)`** : `TransfertStatutChange` devient `rabbitTemplate.convertAndSend(cle, RescueOpsEvent.of(cle, payload))`, avec le payload `{transfertId, missionId, serviceId, hopitalId, statut}`. Les clés de routage sont :
  - `transfert.place_retenue`, `transfert.confirme`, `transfert.refuse`, `transfert.expire` ;
  - `capacite.maj` à chaque `ajusterCapacite`, `reserver` ou `rendre` (payload : `{serviceId, placesDisponibles, capaciteTotale}`).
- **Consommation** :
  - file durable `ms-hospital.mission-terminee`, liée à `rescueops.events` avec la clé `mission.terminee`, et dead-letter vers `rescueops.events.dlx` (file `ms-hospital.mission-terminee.dlq`) ;
  - 3 tentatives au maximum (`spring.rabbitmq.listener.simple.retry`) ;
  - le listener reçoit `RescueOpsEvent<Map<String, Object>>`, lit `payload.missionId`, puis **dans une transaction** : insère `evenement_traite(eventId)` (si l'id existe déjà, il s'arrête) et annule les demandes de la mission qui sont en `DEMANDEE` ou `PLACE_RETENUE`.
- La confiance Jackson se limite aux paquets `tn.esprit.rescueops.*` et `java.util.*`, avec le **type inféré** depuis la signature du listener.

- [ ] **Étape 1 :** tests `MissionTermineeListenerTest` (Mockito) : `annuleLesDemandesDeLaMission` et `memeEventIdTraiteUneSeuleFois`. Tests `HospitalEventPublisherTest` : `confirmationPublieTransfertConfirme` et `rienNEstPublieSiLaTransactionEchoue`.
- [ ] **Étape 2 :** RED, implémentation, GREEN.
- [ ] **Étape 3 :** commit `feat(hospital): événements RabbitMQ (publication après commit, consommateur idempotent)`.

---

### Task 5 : intégration réelle (Testcontainers MySQL et RabbitMQ) et données de démo

**Fichiers :**
- `HospitalIntegrationTest` ;
- `db/demo/V100__donnees_demo.sql`, chargé seulement si `spring.flyway.locations` inclut `classpath:db/demo` (profil `demo`). Il contient 8 hôpitaux fictifs du Grand Tunis avec coordonnées, et 2 à 4 services chacun.

- [ ] **Étape 1 : écrire les tests :**
  - `cinquanteReservationsPourDixPlaces` : 50 threads, 50 demandes, 1 service de 10 places → 10 réponses 200, 40 réponses 409 `CAPACITE_EPUISEE`, `places_disponibles = 0` en base ;
  - `expirationEtRefusSimultanesRendentUneSeulePlace` : 2 threads, `expirer` et `refuser` sur la même demande → exactement un succès, et places = initial ;
  - `migrationEtValidationHibernate` ;
  - `evenementPublieApresCommit` : un consommateur de test lié à `transfert.#` reçoit `transfert.confirme` ;
  - `missionTermineeDeuxFoisUnSeulEffet` : publication réelle de 2 messages avec le même `eventId`.
- [ ] **Étape 2 :** `./mvnw -pl services/ms-hospital test` (Docker requis) → tout est vert.
- [ ] **Étape 3 :** commit `test(hospital): intégration MySQL et RabbitMQ réels (concurrence, expiration, idempotence)`.

---

### Task 6 : branchement au socle et vérification de bout en bout

- [ ] **Étape 1 :** `README.md` du service, construit comme celui de `ms-user` (routes, rôles, scénario de démo en PowerShell avec `Invoke-RestMethod`), plus une mention dans `docs/GUIDE-EQUIPE.md`. **Le service n'est pas ajouté à `docker-compose.infra.yml`** : comme les modules des coéquipiers, il se lance depuis IntelliJ en profil `local`. Le profil `local,demo` charge les données fictives.
- [ ] **Étape 2 :** lancer `ms-hospital` en `local,demo`, puis dérouler le scénario avec de vrais tokens :
  1. l'opérateur crée une demande (CARDIOLOGIE, CRITIQUE, coordonnées de Tunis) ;
  2. `compatible` renvoie une liste triée avec justifications ;
  3. l'ambulancier réserve ;
  4. le coordinateur confirme, puis admet, puis déclare la sortie ;
  5. les places reviennent à leur valeur initiale ;
  6. la console RabbitMQ montre les messages `transfert.*` ;
  7. le ticker `feed` liste les transitions.
- [ ] **Étape 3 :** revue finale indépendante et corrections (avec un test qui échoue d'abord pour chacune).
- [ ] **Étape 4 :** commit. **Push seulement sur accord explicite du porteur.**
