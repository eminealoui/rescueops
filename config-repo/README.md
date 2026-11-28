# config-repo : configuration centralisée RescueOps

Ce dossier est servi par le **Config Server** (`infrastructure/config-server`, port 8888).

| Fichier | Lu par |
|---|---|
| `application.yml` | **tous** les services (réglages communs) |
| `ms-xxx.yml` | le seul service `ms-xxx` |
| `ms-xxx-docker.yml` | `ms-xxx` avec le profil `docker` (optionnel) |

Règles :
- une valeur d'ici **remplace** celle du `application.yml` du service ; n'y mettre que ce qui doit être commun ou centralisé ;
- **aucun secret** ici (mots de passe, clés) : ils restent dans `.env` et les variables d'environnement ;
- le service démarre quand même si le Config Server est arrêté (`optional:configserver:`), avec sa config locale.

Vérifier ce que reçoit un service : <http://localhost:8888/ms-hospital/default>
