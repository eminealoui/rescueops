# Plan 5 : frontend Angular (coquille commune, design bleu pétrole, User, Hospital)

> **Pour les agents qui exécutent ce plan :** SOUS-SKILL REQUIS : superpowers:executing-plans (exécution « Native »). Étapes à cocher (`- [ ]`).

**But :** une application Angular unique, `frontend/rescueops-web`, qui reproduit fidèlement la maquette validée (`docs/design/maquette-v3.html`, bleu pétrole, mode clair ou sombre). Elle :
- se connecte à Keycloak (Authorization Code + PKCE) ;
- appelle uniquement la Gateway ;
- livre les écrans **Profil**, **Administration des utilisateurs** et **Coordination hospitalière** ;
- fournit une coquille et un design system que les 5 coéquipiers réutilisent pour leurs écrans.

**Stack :**
- Angular **22.3** : standalone, signals, nouveau control flow, `OnPush`, SCSS ;
- `angular-oauth2-oidc` **22.0.2** ;
- `@angular/cdk` **22.3** (overlay, a11y) ;
- Leaflet **1.9.4** avec les tuiles OpenStreetMap (gratuites, sans clé, avec attribution ; sombres par filtre CSS). Les tuiles CARTO demandent désormais une clé ;
- tests : le runner par défaut d'Angular 22 ;
- Node 22.22.3.

**Spec :** §10 de `docs/superpowers/specs/2026-11-21-rescueops-socle-hospital-design.md`, `docs/design/README.md` et `docs/design/tokens.css`.

## Contraintes globales
- **URLs** :
  - application `http://localhost:4200` ;
  - API **toujours** via la Gateway, en chemin relatif `/api/...` : `ng serve` relaie vers la Gateway (`proxy.conf.mjs`, port lu dans `.env` : `GATEWAY_HOST_PORT`, 8080 par défaut), jamais un port de microservice ;
  - Keycloak `http://localhost:8180/realms/rescueops`, client public `rescueops-web` (PKCE S256), déjà configuré dans le realm.
- **Aucun secret** dans le code Angular. Le token n'est envoyé **qu'à** `/api` (même origine, liste blanche de l'intercepteur). Aucun token dans les logs.
- **Les rôles** sont lus dans `realm_access.roles` de l'access token. Les guards ne servent **qu'à l'ergonomie** : la sécurité réelle reste côté API.
- **Design** :
  - toutes les couleurs passent par les variables de `tokens.css` (copié dans `src/styles/tokens.css`) ;
  - aucune couleur en dur dans les composants ;
  - polices IBM Plex Sans, IBM Plex Mono et Barlow Condensed ;
  - vert, ambre et rouge **réservés aux statuts** ;
  - `prefers-reduced-motion` respecté ;
  - lisible à 375 px de large.
- **Erreurs** : une réponse ProblemDetail affiche son `detail` dans un toast, et jamais un message technique. 401 relance la connexion, 403 affiche la page « Accès refusé », 503 affiche « Service momentanément indisponible ».
- **Langue** : interface en **français**, et dates au format `fr-TN`.

## Structure
```
frontend/rescueops-web/src/
├── styles/                tokens.css (copie de docs/design) + styles.scss (base, typo)
├── app/
│   ├── core/
│   │   ├── auth/          auth.config.ts, auth.service.ts (signals : user, roles, isAuthenticated), role.guard.ts
│   │   ├── http/          api.interceptor.ts (Bearer + X-Correlation-Id), error.interceptor.ts (ProblemDetail → toast)
│   │   ├── theme/         theme.service.ts (clair/sombre, mémorisé, préférence système par défaut)
│   │   └── api/           user.api.ts, hospital.api.ts, transfer.api.ts (clients typés) + models.ts
│   ├── design-system/     button, card, tag, chip-filter, kpi-band, drawer, toast, score-ring,
│   │                      countdown, timeline-steps, ticker, segmented-tabs, map (Leaflet), empty-state
│   ├── layout/            shell (bandeau EN DIRECT, en-tête, navigation par rôle, thème, avatar)
│   ├── features/
│   │   ├── accueil/       tableau d'accueil selon les rôles
│   │   ├── profile/       mon profil + édition
│   │   ├── admin-users/   liste paginée, recherche, activer/désactiver (ADMIN)
│   │   ├── hospital/      capacites (héro + cartes + filtres), etablissements (CRUD coordinateur),
│   │   │                  transferts (liste + tiroir de traitement), nouvelle-demande (compatibles + carte),
│   │   │                  admissions (CONFIRMEE/ADMIS → admettre/sortie)
│   │   ├── modules/       page « module en construction » pour emergency, resource, dispatch, intervention, training
│   │   └── errors/        acces-refuse, introuvable, indisponible
│   └── app.routes.ts      chargement paresseux + roleGuard
└── environments/          environment.ts (URLs locales)
```

## Points de vigilance
1. **Token expiré pendant que l'utilisateur travaille** : il est renouvelé silencieusement avec le refresh token. Si le renouvellement échoue, retour à la page de connexion **sans boucle infinie**. Test sur l'intercepteur.
2. **Le token ne part jamais vers un autre domaine** (tuiles CARTO, Google Fonts). Test : une requête vers `https://tile...` n'a pas d'en-tête `Authorization`.
3. **Un double clic sur « Réserver » ou « Confirmer »** : le bouton est désactivé pendant la requête. Le backend renverrait de toute façon 409, et le toast l'expliquerait.
4. **Un utilisateur sans rôle opérationnel** (CITOYEN) : ni ticker, ni menu « Hôpitaux » en écriture. Les écrans s'affichent sans erreur 403 en boucle.
5. **API indisponible** (Gateway arrêtée) : page ou toast « Service indisponible ». L'application ne reste jamais sur un écran blanc.

---

### Task 0 : demandes de rôle professionnel (backend ms-user), validées par le porteur le 22/11
- **Règle** : l'inscription publique donne **toujours** le rôle CITOYEN. Un rôle professionnel (OPERATEUR, GESTIONNAIRE_RESSOURCES, AMBULANCIER, COORDINATEUR_HOSPITALIER, FORMATEUR, APPRENANT, mais **jamais ADMIN**) se **demande**, puis un ADMIN l'accorde.
- **Migration `V3__demande_role.sql`** : table `demande_role` (id, keycloak_id, role_demande, organisation, motif, statut `EN_ATTENTE` / `ACCEPTEE` / `REFUSEE`, traite_par, motif_refus, created_at, traite_le, version). Une seule demande `EN_ATTENTE` par utilisateur.
- **Routes** :
  - `POST /api/users/me/role-requests` et `GET /api/users/me/role-requests` (authentifié) ;
  - `GET /api/users/role-requests?statut=` (ADMIN) ;
  - `POST /api/users/role-requests/{id}/approve` et `/reject` (ADMIN).
- **Attribution** : `KeycloakAdminClient` obtient un token de compte de service (client confidentiel `ms-user-admin`, avec les rôles `manage-users` et `view-realm`, secret lu dans `.env`), puis ajoute le rôle de realm à l'utilisateur. Keycloak d'abord, puis la base : une nouvelle approbation reste sans effet, et si Keycloak ne répond pas, on renvoie 503 `KEYCLOAK_INDISPONIBLE` sans rien modifier.
- **Tests** : demande d'un rôle ADMIN → 400 ; deuxième demande en attente → 409 ; approbation par un non-admin → 403 ; l'approbation appelle Keycloak puis passe la demande à `ACCEPTEE` ; Keycloak indisponible → 503 avec la demande toujours `EN_ATTENTE`.

### Task 0bis : page d'accueil publique (maquette `docs/design/accueil-v1.html`, validée le 22/11)
- **Route `/`**, publique, qui reproduit fidèlement la maquette : bandeau EN DIRECT (données de démonstration), héro avec carte animée, chiffres clés, parcours interactif en 5 étapes, 6 modules, sélecteur « Pour qui ? », appel final.
- **« Se connecter »** lance `initCodeFlow()`. **« Créer un compte »** lance `initCodeFlow` avec `prompt=create`, qui ouvre la page d'inscription Keycloak. Un utilisateur déjà connecté est redirigé vers `/accueil`.

### Task 1 : projet, design system, coquille, connexion Keycloak
- [ ] `npx -y @angular/cli@22.3.0 new rescueops-web --directory frontend/rescueops-web --style=scss --routing --ssr=false --skip-git`, puis `npm i angular-oauth2-oidc@22.0.2 @angular/cdk@22.3.0 leaflet@1.9.4` et `npm i -D @types/leaflet`.
- [ ] Copier `tokens.css`, puis écrire `styles.scss`, le `ThemeService`, le `AuthService` (code flow PKCE, `setupAutomaticSilentRefresh`, rôles en signal), les deux intercepteurs et le `roleGuard`.
- [ ] **Tests d'abord**, qui échouent puis passent :
  - `auth.service.spec` : extraction des rôles RescueOps (et pas des rôles techniques) ;
  - `role.guard.spec` : un rôle manquant redirige vers `/acces-refuse` ;
  - `api.interceptor.spec` : Bearer seulement vers `localhost:8080` ;
  - `error.interceptor.spec` : 409 ProblemDetail → toast avec le `detail`, 503 → « indisponible ».
- [ ] Composants du design system et coquille (`shell`) fidèles à la maquette : bandeau, en-tête, onglets à pastille, bouton de thème, toasts.
- [ ] Vérification : `npm run build` réussit, `npm test` est vert.

### Task 2 : profil et administration des utilisateurs
- [ ] **Profil** : `GET` et `PUT /api/users/me` en Reactive Forms typé (prénom et nom obligatoires, téléphone au format E.164 facultatif), toast « Profil enregistré ».
- [ ] **Administration** (ADMIN) : recherche avec délai de saisie, tableau paginé (20 par page), tri, bascule actif/inactif avec confirmation.
- [ ] Tests : validation du formulaire de profil, et appel de `search` avec les bons paramètres.

### Task 3 : coordination hospitalière
- [ ] **Capacités** : héro « Voir / Anticiper / Coordonner / Agir » (`/api/hospitals/capacities`), cartes de services avec barre animée, filtres en pastilles, mode clair et sombre.
- [ ] **Nouvelle demande** (AMBULANCIER, OPERATEUR) : formulaire mission, spécialité, gravité et position (clic sur la carte Leaflet), puis la liste des **compatibles** avec jauge de score et justification, puis **Réserver**.
- [ ] **Transferts** : liste filtrable par statut. Un clic ouvre le **tiroir** : carte avec trajet, frise d'étapes, minuteur avant expiration, et actions selon le rôle et le statut (confirmer, refuser avec motif, annuler).
- [ ] **Admissions** (coordinateur) : demandes CONFIRMEE puis ADMIS, avec les boutons Admettre et Sortie.
- [ ] **Établissements** (coordinateur) : CRUD hôpital et service, ajustement de capacité avec motif. Une erreur 422 est affichée clairement.
- [ ] **Ticker EN DIRECT** : `/api/transfers/feed` toutes les 15 s, rôles opérationnels uniquement, en pause au survol.
- [ ] Tests : la frise d'étapes (statut → étape active), le minuteur (`expireAt` → mm:ss, puis « expirée ») et l'affichage des actions selon le rôle et le statut.

### Task 4 : vérification réelle et revue
- [ ] `ng serve`, avec le socle et `ms-hospital` en `local,demo`.
- [ ] Dans le navigateur intégré, avec les comptes de démo, dérouler le **scénario complet** :
  1. l'opérateur crée une demande ;
  2. l'ambulancier réserve ;
  3. le coordinateur confirme, admet puis déclare la sortie ;
  4. les capacités reviennent à leur valeur initiale ;
  5. le citoyen ne voit ni le ticker ni l'administration.
- [ ] Captures en **clair et sombre**, et à **375 px** de large.
- [ ] Revue finale indépendante, corrections (un test d'abord pour chacune), commit. **Push seulement sur accord.**
