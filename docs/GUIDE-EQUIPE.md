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

## Les règles de l'équipe

1. **On ne modifie jamais** la Gateway, `rescueops-common`, le realm Keycloak ou le docker-compose sans en parler à l'équipe.
2. **Une branche par personne** (`feature/ms-xxx`), puis une Pull Request vers `develop`. Jamais de push direct sur `develop` ou `main`.
3. **Une base par service** : on n'accède jamais aux tables d'un autre service.
4. **Les 8 rôles officiels uniquement** : `CITOYEN`, `OPERATEUR`, `GESTIONNAIRE_RESSOURCES`, `AMBULANCIER`, `COORDINATEUR_HOSPITALIER`, `FORMATEUR`, `APPRENANT`, `ADMIN`.
5. **L'utilisateur est identifié par son `sub` Keycloak**, avec `CurrentUser.current().keycloakId()`.
6. **Pas de secret dans Git** : le fichier `.env` reste sur ton PC.
