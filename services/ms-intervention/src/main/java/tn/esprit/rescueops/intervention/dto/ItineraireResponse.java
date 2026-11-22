package tn.esprit.rescueops.intervention.dto;

import tn.esprit.rescueops.intervention.domain.TypeItineraire;

import java.time.Instant;
import java.util.UUID;

public record ItineraireResponse(

    UUID missionId,
    TypeItineraire typeItineraire,

    Double latitudeDepart,
    Double longitudeDepart,

    Double latitudeDestination,
    Double longitudeDestination,

    Double distanceKm,
    Integer dureeEstimeeMinutes,

    Instant itineraireModifieLe

) {
}
