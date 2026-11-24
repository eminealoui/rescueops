package tn.esprit.rescueops.dispatch.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requête pour créer une affectation (BF-08).
 * L'opérateur sélectionne une recommandation et crée l'affectation.
 *
 * @param incidentId  identifiant de l'incident à couvrir
 * @param ressourceId identifiant de la ressource choisie
 */
public record AffectationRequest(
        @NotBlank(message = "L'identifiant de l'incident est obligatoire")
        String incidentId,

        @NotBlank(message = "L'identifiant de la ressource est obligatoire")
        String ressourceId,

        /** Type de ressource (optionnel, déduit si absent) : AMBULANCE, EQUIPE, EQUIPEMENT */
        String typeRessource
) {}
