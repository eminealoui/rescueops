package tn.esprit.rescueops.dispatch.dto;

import java.time.Instant;

/**
 * Représentation publique d'une recommandation de scoring (BF-07).
 * Retournée en liste triée par score décroissant.
 *
 * @param id              identifiant interne
 * @param incidentId      identifiant de l'incident
 * @param ressourceId     identifiant de la ressource
 * @param nomRessource    nom lisible de la ressource
 * @param typeRessource   type : AMBULANCE, EQUIPE, EQUIPEMENT
 * @param score           score global [0, 1]
 * @param justification   explication détaillée du score
 * @param distanceKm      distance en km entre la ressource et l'incident
 * @param disponible      la ressource est-elle disponible ?
 * @param competencesOk   la ressource a-t-elle les compétences requises ?
 * @param createdAt       moment du calcul
 */
public record RecommandationResponse(
        Long id,
        String incidentId,
        String ressourceId,
        String nomRessource,
        String typeRessource,
        Double score,
        String justification,
        Double distanceKm,
        Boolean disponible,
        Boolean competencesOk,
        Instant createdAt
) {}
