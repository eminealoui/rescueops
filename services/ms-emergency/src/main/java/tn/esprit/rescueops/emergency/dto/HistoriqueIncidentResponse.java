package tn.esprit.rescueops.emergency.dto;

import java.time.Instant;
import java.util.UUID;

import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeActionIncident;

public record HistoriqueIncidentResponse(

        UUID id,
        UUID incidentId,
        TypeActionIncident action,
        StatutIncident ancienStatut,
        StatutIncident nouveauStatut,
        String auteur,
        String details,
        Instant dateAction

) {
}