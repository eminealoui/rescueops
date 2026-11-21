
package tn.esprit.rescueops.intervention.dto;

import jakarta.validation.constraints.NotBlank;

public record RapportRequest(

    @NotBlank
    String bilan,

    @NotBlank
    String actionsRealisees,

    String observations

) {
}
