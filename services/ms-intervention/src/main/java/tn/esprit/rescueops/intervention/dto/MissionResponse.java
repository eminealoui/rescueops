
package tn.esprit.rescueops.intervention.dto;

import tn.esprit.rescueops.intervention.domain.StatutMission;

import java.time.Instant;
import java.util.UUID;

public record MissionResponse(
    UUID id,
    String commentaire,
    StatutMission statut,
    Instant creeLe
) {
}
