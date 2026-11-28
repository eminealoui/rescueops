package tn.esprit.rescueops.emergency.dto;

import java.time.Instant;
import java.util.UUID;

import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeIncident;

public record DoublonResponse(

        UUID incidentId,
        String titre,
        String description,
        TypeIncident type,
        StatutIncident statut,
        Instant creeLe,

        double distanceMetres,
        long ecartMinutes,
        double similariteTexte,
        int scoreSimilarite,
        String recommandation

) {
}