
package tn.esprit.rescueops.intervention.dto;

import java.time.Instant;
import java.util.UUID;

public record HistoriqueMissionResponse(
    UUID id,
    String ancienStatut,
    String nouveauStatut,
    Instant dateChangement,
    String modifiePar
) {
}
