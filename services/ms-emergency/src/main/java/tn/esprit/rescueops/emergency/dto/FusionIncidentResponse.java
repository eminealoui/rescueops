package tn.esprit.rescueops.emergency.dto;

import java.time.Instant;
import java.util.UUID;

import tn.esprit.rescueops.emergency.domain.StatutIncident;

public record FusionIncidentResponse(

        UUID incidentPrincipalId,
        UUID incidentSecondaireId,

        StatutIncident statutPrincipal,
        StatutIncident statutSecondaire,

        String motif,
        Instant fusionneLe

) {
}