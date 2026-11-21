
package tn.esprit.rescueops.intervention.dto;

import java.time.Instant;
import java.util.UUID;

public record RapportResponse(

    UUID id,

    UUID missionId,

    String bilan,

    String actionsRealisees,

    String observations,

    String creePar,

    Instant creeLe

) {
}
