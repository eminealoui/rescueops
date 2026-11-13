# RescueOps — Plateforme de coordination intelligente des urgences

Projet AWD (Architecture Web Distribuée) — validation intermédiaire (séance 4).
Application web de simulation (données fictives) en **6 microservices Spring Boot** et un frontend **Angular**.

## Structure du dépôt

```
rescueops-4das16-g3/
├── Backend/                  # api-gateway, eureka-server, 6 microservices, docker-compose.yml
├── Frontend/                 # application Angular
├── Documentation/
│   ├── Presentation/         # présentation de la séance 4
│   ├── Diagrams/             # sources PlantUML (.puml) et rendus PNG
│   └── Dossier_technique/    # dossier technique (Word)
└── README.md
```

## Microservices

| Service | Port | Rôle |
|---|---|---|
| MS1 emergency-service | 8081 | Incidents |
| MS2 resource-service | 8082 | Ambulances, équipes, équipements |
| MS3 dispatch-service | 8083 | Recommandations, affectations |
| MS4 intervention-service | 8084 | Missions, rapports |
| MS5 hospital-service | 8085 | Hôpitaux, transferts |
| MS6 training-service | 8086 | Formations, certifications |
| api-gateway / eureka / Keycloak | 8080 / 8761 / 8180 | Entrée unique, découverte, authentification |

## Lancement

```bash
cd Backend
docker compose up --build
```

## Équipe

À compléter : noms des membres, classe, groupe, microservice de chacun. L'enseignant est ajouté comme collaborateur du dépôt.
