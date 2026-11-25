package tn.esprit.rescueops.dispatch.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

/**
 * Corps de la requête pour créer une affectation (BF-08).
 * L'opérateur sélectionne une recommandation et crée l'affectation.
 *
 * @param incidentId  identifiant de l'incident à couvrir
 * @param ressourceId identifiant de la ressource choisie
 * @param typeRessource type de ressource (optionnel, déduit si absent) : AMBULANCE, EQUIPE, EQUIPEMENT
 * @param score       score de la proposition (optionnel, entre 0 et 1 comme dans ScoringService)
 */
public record AffectationRequest(
        @NotBlank(message = "L'identifiant de l'incident est obligatoire")
        String incidentId,

        @NotBlank(message = "L'identifiant de la ressource est obligatoire")
        String ressourceId,

        String typeRessource,

        @DecimalMin(value = "0.0", message = "Le score doit être compris entre 0 et 1")
        @DecimalMax(value = "1.0", message = "Le score doit être compris entre 0 et 1")
        Double score
) {
    /** Constructeur historique sans score, conservé pour les appelants/tests existants. */
    public AffectationRequest(String incidentId, String ressourceId, String typeRessource) {
        this(incidentId, ressourceId, typeRessource, null);
    }
}
