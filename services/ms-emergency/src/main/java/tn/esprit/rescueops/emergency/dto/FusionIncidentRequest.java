package tn.esprit.rescueops.emergency.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FusionIncidentRequest(

        @NotNull(message = "Incident secondaire obligatoire")
        UUID incidentSecondaireId,

        @NotBlank(message = "Le motif est obligatoire")
        @Size(max = 500)
        String motif

) {
}