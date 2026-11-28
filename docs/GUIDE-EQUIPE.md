# 👋 Bienvenue dans l'équipe RescueOps

> **Tu fais partie de l'équipe ? Commence ici.**
> 1. Repère ton module dans le tableau ci-dessous.
> 2. Clique sur **ton guide** : il t'emmène pas à pas, de l'installation jusqu'au code de ton microservice.
> 3. Suis les étapes **dans l'ordre**, sans en sauter.

## Choisis ton module

| Module | Microservice | Port | 📘 Ton guide |
|---|---|---|---|
| 1. Gestion des urgences | `ms-emergency` | 8081 | **[services/ms-emergency/README.md](../services/ms-emergency/README.md)** |
| 2. Gestion des ressources | `ms-resource` | 8082 | **[services/ms-resource/README.md](../services/ms-resource/README.md)** |
| 3. Dispatching intelligent | `ms-dispatch` | 8083 | **[services/ms-dispatch/README.md](../services/ms-dispatch/README.md)** |
| 4. Gestion des interventions | `ms-intervention` | 8084 | **[services/ms-intervention/README.md](../services/ms-intervention/README.md)** |
| 5. Coordination hospitalière | `ms-hospital` | 8085 | **[services/ms-hospital/README.md](../services/ms-hospital/README.md)** (service de référence) |
| 6. Formations et certifications | `ms-training` | 8086 | **[services/ms-training/README.md](../services/ms-training/README.md)** |
| Transversal. Utilisateurs | `ms-user` | 8087 | **[services/ms-user/README.md](../services/ms-user/README.md)** (déjà inclus dans le socle) |

## Comment le projet est construit (en 30 secondes)

```
                 🔐 Keycloak : l'accueil, qui donne les badges (tokens)
                          │
  🌐 Navigateur ──► 🚪 API Gateway : la porte d'entrée, qui vérifie les badges
                          │
                 📒 Eureka : l'annuaire des services
                          │
     🏢 Les 7 microservices : un par membre, chacun avec SA base MySQL
                          │
                 📮 RabbitMQ : la poste interne entre services
```

**Tout le socle (Keycloak, Gateway, Eureka, MySQL, RabbitMQ) est déjà prêt** et se lance avec une seule commande Docker.
Ton travail : **coder la logique métier de ton microservice**. Ton squelette est déjà branché sur tout le reste.

## L'application web (Angular) : une seule app pour toute l'équipe

L'interface est **une seule application Angular**, dans [`frontend/rescueops-web`](../frontend/rescueops-web/README.md). Elle contient déjà :
- la page d'accueil publique, la connexion et l'inscription (pages Keycloak aux couleurs de RescueOps) ;
- la **coquille commune** : bandeau EN DIRECT, en-tête avec logo, onglets selon les rôles, mode clair ou sombre, menu du compte ;
- le profil, les demandes de rôle et l'administration ;
- le module 5 (Hôpitaux), **à prendre comme exemple** pour tes écrans.

**Tu ne crées pas ta propre application** : tu ajoutes tes écrans dans celle-ci.

### Lancer l'application

Prérequis : Node 22, et le socle démarré (`docker compose -f docker-compose.infra.yml up -d`).

```powershell
cd frontend/rescueops-web
npm install
npm start
```

Ouvre http://localhost:4200. Comptes de démo : `admin@rescueops.test`, `operateur@rescueops.test`, `ambulancier@rescueops.test`… mot de passe `Demo1234!`.

- Si `npm start` demande « use a different port? », réponds **n** : Keycloak n'accepte que le retour vers `localhost:4200`.
- L'application appelle toujours `/api/...`, et `npm start` relaie vers la Gateway. Si le port 8080 est pris sur ton PC (Oracle, Tomcat…), mets `GATEWAY_HOST_PORT=8090` dans ton `.env`, puis relance la Gateway (`docker compose -f docker-compose.infra.yml up -d api-gateway`) et `npm start`. Choisis un port **libre et différent de ceux des microservices** (8081 à 8087).

### Brancher les écrans de TON module (4 étapes)

1. Crée ton dossier `src/app/features/<ton-module>/` avec un fichier `<ton-module>.routes.ts` (modèle : `features/hospital/hospital.routes.ts`).
2. Dans `src/app/app.routes.ts`, ajoute ton bloc `loadChildren` dans les routes de la coquille, protégé par `roleGuard('TON_ROLE', ...)`.
3. Dans `src/app/layout/navigation.ts`, remplace la ligne `/modules/msX` de ton module par ta route. Ton onglet apparaît alors pour les bons rôles.
4. Crée ton client d'API dans `src/app/core/api/` (modèle : `hospital.api.ts`), avec des chemins `environment.apiBase + '/ta-ressource'`. **Jamais d'URL complète, jamais de port de microservice.**

### Ce qui existe déjà : réutilise-le, ne le recode pas

| Besoin | À utiliser |
|---|---|
| Couleurs, polices | les variables de `src/styles/tokens.css` (`var(--brand)`, `var(--ok)`…). Vert, ambre et rouge sont **réservés aux statuts** |
| Boutons, cartes, étiquettes, filtres, champs | les classes `.btn .btn-primary .btn-outline`, `.card`, `.tag .tag-ok/.tag-warn/.tag-danger`, `.chip`, `.input .select .field` |
| Message de succès | `ToastService` (`toast.ok('Enregistré')`) |
| Confirmation avant une action irréversible | `Confirm` (`confirm.demander({ titre, texte, action })`) |
| Erreurs HTTP | **rien à faire** : l'intercepteur affiche le message de l'API, gère 401, 403 et « service indisponible » |
| Utilisateur connecté, rôles | `AuthService` (`auth.roles()`, `auth.hasRole('ADMIN')`, `auth.utilisateur()`) |
| Carte | `<app-carte>` (Leaflet, tuiles OpenStreetMap gratuites) |
| Logo | `<app-brand>`. Le fichier source est [design/logo.svg](design/logo.svg) |

**Avant chaque Pull Request** : `npm test` et `npm run build` doivent passer, et ton écran doit rester lisible à 375 px de large et en mode sombre.

## Les documents de référence

| Document | Pour quoi |
|---|---|
| [README du projet](../README.md) | Vue d'ensemble, comptes de démo, dépannage |
| [Charte de design](design/README.md) | Couleurs (bleu pétrole), polices, composants, règles |
| [Maquette validée](design/maquette-v3.html) | Le modèle visuel à suivre (à télécharger et ouvrir dans le navigateur) |
| [Document de conception](superpowers/specs/2026-11-21-rescueops-socle-hospital-design.md) | Toutes les décisions d'architecture |

## Qui parle à qui (événements RabbitMQ)

| Service | Publie | Écoute |
|---|---|---|
| ms-emergency | `incident.priorise` | `affectation.confirmee` |
| ms-resource | `ressource.reservee` / `ressource.liberee` | `mission.terminee` |
| ms-dispatch | `affectation.confirmee` | `incident.priorise` (+ appel REST vers ms-resource) |
| ms-intervention | `mission.statut`, `mission.terminee` | `affectation.confirmee`, `transfert.confirme` |
| ms-hospital | `transfert.confirme`, `transfert.expire`... | `mission.terminee` |
| ms-training | `certification.maj` | |

## 🆕 Mise à jour du socle : ce que tu dois faire (5 minutes)

Le socle a reçu : un **Config Server**, un **docker-compose complet**, un **load balancer Nginx** (HTTPS, rate limit, en-têtes de sécurité), des **logs centralisés** et un **monitoring** (Grafana). **Ton code métier ne change pas.**

### 1. Récupérer la mise à jour dans ta branche

```powershell
git checkout feature/ms-xxx
git pull origin develop
```

Une seule ligne de **ton** `application.yml` a changé (celle de `spring.config.import`) :

```yaml
spring:
  config:
    import: classpath:rescueops-logging.properties,optional:configserver:${CONFIG_SERVER_URL:http://localhost:8888}
```

### 2. Si Git annonce un conflit sur `application.yml`

C'est que tu as modifié la même ligne. Ouvre le fichier, garde **la version qui contient `optional:configserver:`**, ajoute-y ce que tu avais ajouté toi-même, puis :

```powershell
git add services/ms-xxx/src/main/resources/application.yml
git commit
```

En cas de doute, demande avant de pousser : ne supprime jamais la partie `optional:configserver:`.

### 3. Ce qui ne change pas pour toi

- Tu lances toujours le socle avec `docker compose -f docker-compose.infra.yml up -d`, puis **ton** service dans IntelliJ (profil `local`).
- Ton port, ta base, tes routes `/api/...`, tes rôles et tes événements RabbitMQ sont identiques.
- Le Config Server est **optionnel** (`optional:`) : s'il n'est pas lancé, ton service démarre avec sa configuration locale, comme avant.
- Le front se lance toujours avec `npm start` sur http://localhost:4200.

### 4. Lancer TOUT le projet dans Docker (démo, soutenance)

```powershell
docker compose up -d --build
```

- Application : **https://localhost:8443** (http://localhost:8090 y redirige)
- Premier lancement : 5 à 10 minutes (compilation). Avec une connexion lente, construis les images une par une : `docker compose build ms-xxx`.
- Le navigateur peut afficher « connexion non privée » : c'est le certificat auto-signé de développement. Clique sur *Paramètres avancés → Continuer*. Pour supprimer l'alerte (facultatif, gratuit) : installe [mkcert](https://github.com/FiloSottile/mkcert), puis `mkcert -install` et `mkcert -cert-file infrastructure/nginx/certs/localhost.pem -key-file infrastructure/nginx/certs/localhost-key.pem localhost 127.0.0.1`, puis `docker compose restart lb`.
- Monitoring (gourmand en mémoire) : `docker compose --profile monitoring up -d`, puis **http://localhost:3000** (1re connexion : `admin` / `admin`, Grafana demande aussitôt un nouveau mot de passe).
- Pas les deux en même temps : `docker compose down` avant de revenir au socle seul.

### 5. Bon à savoir

- **Configuration centralisée** : `config-repo/ms-xxx.yml` est servi à ton service par le Config Server (port 8888). Une valeur mise là **remplace** celle de ton `application.yml`. Jamais de secret dedans.
- **Keycloak durci** : compte bloqué 1 min (jusqu'à 15 min) après 5 mots de passe faux, mots de passe plus stricts. Les comptes de démo (`Demo1234!`) sont conformes. Le nouveau réglage n'arrive dans **ton** Keycloak qu'en le recréant (`docker compose -f docker-compose.infra.yml down`, puis `docker volume rm rescueops_keycloak-data`, puis `up -d`) : tu perds alors les comptes que tu as créés toi-même. C'est facultatif en développement.
- **Rate limit** : au-delà de 20 requêtes/s depuis la même adresse, l'API répond `429` (`TROP_DE_REQUETES`). Seulement avec le projet complet (Nginx) : avec le socle seul, la Gateway répond directement, sans limite.
- **Front (CSP)** : si tu modifies le petit script de `src/index.html`, préviens : son empreinte est autorisée dans `infrastructure/nginx/rescueops-headers.inc`.
- **Logs** : écris tes logs normalement (`log.info(...)`), ils arrivent tout seuls dans Grafana et l'identifiant de corrélation y est déjà.

## Les règles de l'équipe

1. **On ne modifie jamais** la Gateway, `rescueops-common`, le realm Keycloak ou le docker-compose sans en parler à l'équipe.
2. **Une branche par personne** (`feature/ms-xxx`), puis une Pull Request vers `develop`. Jamais de push direct sur `develop` ou `main`.
3. **Une base par service** : on n'accède jamais aux tables d'un autre service.
4. **Les 8 rôles officiels uniquement** : `CITOYEN`, `OPERATEUR`, `GESTIONNAIRE_RESSOURCES`, `AMBULANCIER`, `COORDINATEUR_HOSPITALIER`, `FORMATEUR`, `APPRENANT`, `ADMIN`.
5. **L'utilisateur est identifié par son `sub` Keycloak**, avec `CurrentUser.current().keycloakId()`.
6. **Pas de secret dans Git** : le fichier `.env` reste sur ton PC.
