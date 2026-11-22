
package tn.esprit.rescueops.intervention.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModifierMissionRequest(

    @NotBlank(message = "Le commentaire est obligatoire")
    @Size(max = 500)
    String commentaire

) {
}
