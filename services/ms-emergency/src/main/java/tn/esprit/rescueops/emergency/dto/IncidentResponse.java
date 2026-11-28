package tn.esprit.rescueops.emergency.dto;

import java.time.Instant;
import java.util.UUID;

import tn.esprit.rescueops.emergency.domain.NiveauGravite;
import tn.esprit.rescueops.emergency.domain.NiveauPriorite;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeIncident;

public record IncidentResponse(

        UUID id,
        String titre,
        String description,
        TypeIncident type,
        String adresse,
        Double latitude,
        Double longitude,
        Integer nombreVictimes,
        StatutIncident statut,
        Instant creeLe,
        Instant traiteLe,
        String motifRejet,
        Instant clotureLe,

        NiveauGravite gravite,
        Boolean dangerImmediat,
        Boolean personnesVulnerables,
        Integer scorePriorite,
        NiveauPriorite priorite,
        NiveauPriorite prioriteSuggeree,
        String explicationPriorite,
        String motifAjustementPriorite,

        UUID incidentPrincipalId,
        String fusionnePar,
        Instant fusionneLe,
        String motifFusion

) {
}