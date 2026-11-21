
package tn.esprit.rescueops.intervention.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MissionRequest(
    @NotBlank
    @Size(max = 500)
    String commentaire
) {
}
