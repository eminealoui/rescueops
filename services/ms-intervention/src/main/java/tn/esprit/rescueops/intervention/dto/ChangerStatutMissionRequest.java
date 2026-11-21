
package tn.esprit.rescueops.intervention.dto;

import jakarta.validation.constraints.NotNull;
import tn.esprit.rescueops.intervention.domain.StatutMission;

public record ChangerStatutMissionRequest(
    @NotNull StatutMission statut
) {
}
