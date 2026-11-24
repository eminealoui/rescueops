package tn.esprit.rescueops.dispatch.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Requête pour déclencher le calcul de recommandations (BF-07).
 * L'opérateur fournit l'ID de l'incident et les coordonnées GPS pour le scoring de distance.
 *
 * @param incidentId         identifiant de l'incident
 * @param latitudeIncident   latitude GPS de l'incident
 * @param longitudeIncident  longitude GPS de l'incident
 * @param typeRessource      type de ressource recherché (optionnel) : AMBULANCE, EQUIPE, EQUIPEMENT
 */
public record RecommandationRequest(
        @NotBlank(message = "L'identifiant de l'incident est obligatoire")
        String incidentId,

        Double latitudeIncident,
        Double longitudeIncident,
        String typeRessource
) {}
