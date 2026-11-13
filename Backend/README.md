# Backend — microservices RescueOps

| Dossier | Port | Rôle |
|---|---|---|
| `api-gateway` | 8080 | Point d'entrée unique, vérification JWT |
| `eureka-server` | 8761 | Découverte de services |
| `emergency-service` | 8081 | Incidents |
| `resource-service` | 8082 | Ambulances, équipes, équipements |
| `dispatch-service` | 8083 | Recommandations et affectations |
| `intervention-service` | 8084 | Missions et rapports |
| `hospital-service` | 8085 | Hôpitaux et transferts |
| `training-service` | 8086 | Formations et certifications |
| Keycloak | 8180 | Authentification, rôles |

Lancer l'ensemble : `docker compose up --build` (nécessite Docker).
Un service seul : `mvn spring-boot:run` dans son dossier (PostgreSQL, RabbitMQ et Eureka doivent tourner).
