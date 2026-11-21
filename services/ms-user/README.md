# 👤 ms-user : profils utilisateurs (service partagé)

> Service **transversal**, utilisé par toute l'équipe. Il **tourne déjà dans le socle Docker** (`docker-compose.infra.yml`) : tu n'as pas besoin de le lancer toi-même.

| | |
|---|---|
| Nom Eureka | `ms-user` |
| Port | `8087` (non publié : on passe par la Gateway) |
| API | `http://localhost:8080/api/users/...` |
| Base | `user_db`, compte `user_svc` / `dev_user` (port 3307) |

> ⚠️ **Tu veux modifier ms-user et le lancer depuis IntelliJ ?** Arrête d'abord le conteneur, sinon deux instances tournent et la Gateway alterne entre l'ancienne et la nouvelle version :
> ```powershell
> docker compose -f docker-compose.infra.yml stop ms-user
> ```
> Pour le remettre ensuite : `docker compose -f docker-compose.infra.yml up -d ms-user`.

## Ce que fait ce service

- **Keycloak** gère les **comptes, mots de passe et rôles**.
- **ms-user** gère les **profils métier** : nom, téléphone, adresse, organisation, statut actif ou inactif.
- Le lien entre les deux est le **`keycloakId`**, c'est-à-dire le `sub` du token. C'est **l'identifiant utilisateur commun à tous les services**.
- Le profil est **créé automatiquement à la première connexion** (`GET /api/users/me`), à partir du token.
- La base ne contient **ni mot de passe ni rôle**.

## Les routes

| Route | Qui | Ce que ça fait |
|---|---|---|
| `GET /api/users/me` | tout utilisateur connecté | mon profil (créé s'il n'existe pas) |
| `PUT /api/users/me` | tout utilisateur connecté | modifier **mon** prénom, nom, téléphone et adresse (les autres champs envoyés sont ignorés) |
| `GET /api/users/{keycloakId}/summary` | tout utilisateur connecté | **nom affichable** d'un utilisateur, à utiliser dans tes écrans et tes services |
| `GET /api/users?query=&active=&page=0&size=20&sort=lastName,asc` | ADMIN | recherche paginée (100 par page au maximum ; tri sur `lastName`, `firstName`, `email` ou `createdAt`) |
| `GET /api/users/{keycloakId}` | ADMIN | profil complet |
| `PATCH /api/users/{keycloakId}/status` (corps : `{"active": false}`) | ADMIN | désactiver ou réactiver un profil. Un profil désactivé ne peut plus être modifié. |

## Pour les autres microservices : afficher le nom d'un utilisateur

Ton service stocke des identifiants `...UserId` (le `sub`). Pour afficher un nom, appelle `summary` avec OpenFeign :

```java
@FeignClient(name = "ms-user")
public interface UserClient {
    @GetMapping("/api/users/{keycloakId}/summary")
    UserSummary summary(@PathVariable String keycloakId);

    record UserSummary(String keycloakId, String displayName, String organisation) {}
}
```

Le token de l'utilisateur et l'identifiant de corrélation sont transmis automatiquement par `rescueops-common`.

## Essayer (PowerShell, dans le dossier `rescueops`)

1. Prends un badge :

```powershell
$TOKEN = powershell -ExecutionPolicy Bypass -File .\scripts\get-token.ps1 coordinateur@rescueops.test
```

2. Affiche ton profil :

```powershell
Invoke-RestMethod http://localhost:8080/api/users/me -Headers @{ Authorization = "Bearer $TOKEN" }
```

3. Modifie ton téléphone :

```powershell
Invoke-RestMethod -Method Put http://localhost:8080/api/users/me -Headers @{ Authorization = "Bearer $TOKEN" } -ContentType "application/json; charset=utf-8" -Body '{"firstName":"Meriem","lastName":"Ben Salah","phone":"+21698123456"}'
```

## Tests

```powershell
.\mvnw.cmd -f services\ms-user test
```

Ça lance 21 tests. Les 4 tests d'intégration démarrent un **vrai MySQL 8.4** avec Testcontainers, donc **Docker doit tourner**. Ils vérifient notamment que 10 connexions simultanées ne créent qu'un seul profil.
