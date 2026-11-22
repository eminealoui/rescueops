# RescueOps · application web (Angular 22)

Application unique de l'équipe : page d'accueil publique, connexion Keycloak, coquille commune, et les écrans de chaque module.

## Lancer en local

Prérequis : Node 22, et le socle démarré (`docker compose -f docker-compose.infra.yml up -d` à la racine).

```powershell
cd frontend/rescueops-web
npm install
npm start
```

Ouvrir http://localhost:4200. Comptes de démonstration : `*@rescueops.test` / `Demo1234!` (voir le realm Keycloak).

- L'application appelle **toujours** `/api/...` ; `ng serve` relaie vers la Gateway (`proxy.conf.mjs`).
- Le port de la Gateway est lu dans le `.env` racine (`GATEWAY_HOST_PORT`, 8080 par défaut). Si 8080 est pris sur votre PC (Oracle, Tomcat…), mettez par exemple `GATEWAY_HOST_PORT=8081` dans `.env`, puis relancez la Gateway et `npm start`.
- Tests : `npm test`. Build : `npm run build`.

## Ajouter les écrans de VOTRE module

1. Créez `src/app/features/<votre-module>/` avec un fichier `<votre-module>.routes.ts` (modèle : `features/hospital/hospital.routes.ts`).
2. Dans `src/app/app.routes.ts`, ajoutez votre bloc `loadChildren` dans les routes de la coquille, protégé par `roleGuard(...)` avec vos rôles.
3. Dans `src/app/layout/navigation.ts`, remplacez `/modules/msX` par votre route.
4. Créez votre client typé dans `src/app/core/api/` (modèle : `hospital.api.ts`) : chemins `environment.apiBase + '/votre-ressource'`, jamais d'URL complète.

## Règles communes (à respecter pour une application homogène)

- **Couleurs** : uniquement les variables de `src/styles/tokens.css` (`var(--brand)`, `var(--ok)`…). Vert, ambre et rouge sont **réservés aux statuts**.
- **Composants partagés** : classes `.btn`, `.card`, `.tag`, `.chip`, `.input` ; toasts via `ToastService` ; confirmations via `Confirm` ; bandeau `app-ticker`.
- **Erreurs** : ne gérez pas les erreurs HTTP vous-mêmes : l'intercepteur affiche le `detail` de l'API, redirige sur 401/403 et signale les services indisponibles.
- **Sécurité** : le token n'est envoyé qu'à `/api` ; les guards ne servent qu'au confort, l'API reste seule juge des droits.
- **Accessibilité** : boutons `<button>`, libellés de champs, lisible à 375 px de large, `prefers-reduced-motion` respecté.
- **Langue** : interface en français ; dates en `dd/MM/yyyy HH:mm` (la locale fr-TN affiche sinon 12 h).
