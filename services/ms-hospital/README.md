# 🏥 ms-hospital : Module 5, Coordination hospitalière

> Il coordonne les **demandes de transfert** de patients avec les **capacités d'accueil** des hôpitaux.
> C'est le **service de référence** de l'équipe : sa structure (couches, tests, événements) est un modèle à suivre.

| | |
|---|---|
| Nom Eureka | `ms-hospital` |
| Port | `8085` |
| API (via la Gateway) | `http://localhost:8080/api/hospitals/...` et `/api/transfers/...` |
| Base | `hospital_db`, compte `hospital_svc` / `dev_hospital` (port 3307) |
| Besoins couverts | BF-13, BF-14, BF-15 et BNF-05 (zéro double réservation) |

## Lancer le service (depuis le dossier `rescueops`, socle Docker démarré)

```powershell
.\mvnw.cmd -f services\ms-hospital spring-boot:run "-Dspring-boot.run.profiles=local,demo"
```

Le profil `demo` charge **8 hôpitaux fictifs du Grand Tunis**. Sans `demo`, la base démarre vide.
Dans IntelliJ : lance `HospitalServiceApplication` avec **Active profiles** = `local,demo`.

## Le cycle d'une demande de transfert

```
DEMANDEE ──réserver──► PLACE_RETENUE ──confirmer──► CONFIRMEE ──admettre──► ADMIS ──sortie──► SORTI
   │                     │  │  └─ 15 min sans réponse ─► EXPIREE (place rendue)
   │                     │  └──── refuser ────────────► REFUSEE (place rendue)
   └── annuler ─► ANNULEE └──── annuler ──────────────► ANNULEE (place rendue ; possible aussi depuis CONFIRMEE)
```

- **Une place est prise** au passage à `PLACE_RETENUE`. Elle reste prise en `CONFIRMEE` et `ADMIS`.
- **Elle est rendue** au refus, à l'expiration, à l'annulation et à la sortie du patient.
- **Une action impossible** (confirmer une demande déjà refusée, par exemple) renvoie `409 TRANSITION_INTERDITE`.

## Les routes

| Route | Qui |
|---|---|
| `GET /api/hospitals`, `GET /api/hospitals/{id}` | tout utilisateur connecté |
| `POST/PUT/DELETE /api/hospitals...`, `POST /api/hospitals/{id}/services`, `PUT /api/hospitals/services/{id}` | COORDINATEUR_HOSPITALIER, ADMIN |
| `PATCH /api/hospitals/services/{id}/capacity` : `{capaciteTotale, placesDisponibles, motif}` | COORDINATEUR_HOSPITALIER, ADMIN (ajustement tracé) |
| `GET /api/hospitals/capacities?ville=&specialite=&etat=` | tout utilisateur connecté (écran « Capacités ») |
| `GET /api/hospitals/compatible?specialite=&gravite=&lat=&lon=` | OPERATEUR, AMBULANCIER, COORDINATEUR_HOSPITALIER, ADMIN |
| `POST /api/transfers` : `{missionId, specialiteRequise, gravite, latitudeOrigine, longitudeOrigine}` | AMBULANCIER, OPERATEUR, ADMIN |
| `POST /api/transfers/{id}/reserve` : `{serviceId}` | AMBULANCIER, OPERATEUR, COORDINATEUR_HOSPITALIER, ADMIN |
| `POST /api/transfers/{id}/confirm` · `/refuse` (`{motif}`) · `/admit` · `/discharge` | COORDINATEUR_HOSPITALIER, ADMIN |
| `POST /api/transfers/{id}/cancel` | le demandeur, un coordinateur ou un admin |
| `GET /api/transfers?statut=`, `GET /api/transfers/{id}` (avec l'historique) | rôles opérationnels |
| `GET /api/transfers/feed` | rôles opérationnels (ticker « EN DIRECT », sans données personnelles) |

## Le score de compatibilité (recommandation expliquée)

- **Les candidats** : services actifs avec au moins une place, de la spécialité demandée ou `URGENCES_GENERALES` en repli, dans un rayon de 50 km.
- **Les sous-scores, notés sur 100** :
  - distance = `100 × (1 − d / 50 km)` ;
  - disponibilité = `100 × libres / capacité` ;
  - spécialité = 100 si exacte, 40 si repli.
- **Les poids** (distance / disponibilité / spécialité) :
  - **CRITIQUE** : 0,6 / 0,1 / 0,3 (le plus proche d'abord) ;
  - **ÉLEVÉE** : 0,5 / 0,2 / 0,3 ;
  - **MOYENNE** et **FAIBLE** : 0,35 / 0,35 / 0,3.
- **Chaque proposition est justifiée**, par exemple « 3,3 km · 30 % de places libres · spécialité exacte ». La décision reste humaine.

## Comment on garantit zéro double réservation (BNF-05)

1. **La place se prend en une seule requête SQL** : `UPDATE ... SET places = places - 1 WHERE id = ? AND places > 0`. C'est la base qui décide : s'il ne reste qu'une place, une seule requête réussit.
2. **Chaque transition est conditionnée par le statut attendu** (`... WHERE statut = 'PLACE_RETENUE'`). Si une expiration et un refus arrivent en même temps, un seul gagne, et la place n'est rendue qu'une fois.
3. **L'ordre des verrous est fixe** : d'abord la place (le service), ensuite la demande. Dans l'autre ordre, la clé étrangère provoquait des **interblocages MySQL**, constatés pendant les tests.
4. **Une contrainte `CHECK` en base** sert de garde-fou ultime : jamais de places négatives ni supérieures à la capacité.

## Les événements (exchange `rescueops.events`)

| Sens | Clé | Contenu |
|---|---|---|
| 📤 publié | `transfert.place_retenue`, `transfert.confirme`, `transfert.refuse`, `transfert.expire` | `transfertId`, `missionId`, `serviceId`, `statut` |
| 📤 publié | `capacite.maj` | `serviceId`, `specialite`, `placesDisponibles`, `capaciteTotale`, `actif` |
| 📥 consommé | `mission.terminee` (envoyé par ms-intervention) | `missionId` : les demandes ouvertes de la mission sont annulées et les places rendues |

- **Publication** après le commit en base uniquement.
- **Consommation idempotente** : le même `eventId` n'a qu'un seul effet.
- **Après 3 échecs**, un message va dans la file `ms-hospital.mission-terminee.dlq`.

## Tests (Docker requis pour l'intégration)

```powershell
.\mvnw.cmd -f services\ms-hospital test
```

Cette commande lance **49 tests**, dont 5 sur un **vrai MySQL 8.4 et un vrai RabbitMQ 4.3** (Testcontainers) :
- **50 réservations simultanées pour 10 places** : exactement 10 succès, 40 réponses `409 CAPACITE_EPUISEE`, 0 place restante ;
- **expiration et refus simultanés** : une seule transition réussit, et la place n'est rendue qu'une fois ;
- **`mission.terminee` livré deux fois** : une seule annulation ;
- **les événements partent après le commit** ;
- **la migration Flyway correspond aux entités.**

## Limites connues

- **Un coordinateur hospitalier peut agir sur les demandes de tous les hôpitaux.** Le rattachement d'un coordinateur à **son** établissement n'existe pas encore : il faudrait un attribut Keycloak (`hopitalId`) ou un champ dans ms-user.

- **Sans pattern Outbox**, un événement est perdu (et journalisé en erreur) si RabbitMQ est en panne au moment exact de la publication. L'opération en base, elle, reste valide.
- **La distance est calculée à vol d'oiseau** (formule de Haversine), pas par la route.
