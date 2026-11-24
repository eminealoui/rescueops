package tn.esprit.rescueops.dispatch.dto;

import tn.esprit.rescueops.dispatch.domain.StatutAffectation;

import java.time.Instant;

/**
 * Représentation publique d'une affectation (ne jamais exposer l'entité JPA directement).
 *
 * @param id           identifiant interne
 * @param incidentId   identifiant de l'incident
 * @param ressourceId  identifiant de la ressource
 * @param typeRessource type de ressource
 * @param statut       PROPOSEE / CONFIRMEE / ANNULEE
 * @param score        score multicritère [0, 1]
 * @param justification explication du score
 * @param operateurId  keycloakId de l'opérateur (null si PROPOSEE)
 * @param createdAt    date de création
 * @param updatedAt    date de dernière mise à jour
 */
public record AffectationResponse(
        Long id,
        String incidentId,
        String ressourceId,
        String typeRessource,
        StatutAffectation statut,
        Double score,
        String justification,
        String operateurId,
        Instant createdAt,
        Instant updatedAt
) {}
