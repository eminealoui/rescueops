# Frontend — Angular

Application cliente Angular (port 4200) : une interface par rôle, appels HTTP uniquement vers l'API Gateway (`http://localhost:8080`), connexion via Keycloak (OIDC / PKCE).

Modules prévus : `urgences`, `ressources`, `dispatching`, `interventions`, `hopitaux`, `formations`, `administration`.

Initialisation (à lancer une fois dans ce dossier) :

```bash
npx @angular/cli new rescueops-web --directory . --routing --style=scss --skip-git
ng add @angular/material
npm i leaflet keycloak-js
```
