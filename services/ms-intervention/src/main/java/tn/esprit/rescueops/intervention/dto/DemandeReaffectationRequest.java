package tn.esprit.rescueops.intervention.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DemandeReaffectationRequest(

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 500)
    String motif

) {
}
