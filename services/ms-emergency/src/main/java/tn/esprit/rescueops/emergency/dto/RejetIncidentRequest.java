package tn.esprit.rescueops.emergency.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejetIncidentRequest(

        @NotBlank(message = "Le motif du rejet est obligatoire")
        @Size(max = 500)
        String motif

) {
}