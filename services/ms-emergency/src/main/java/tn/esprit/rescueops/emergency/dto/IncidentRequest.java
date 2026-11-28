package tn.esprit.rescueops.emergency.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.rescueops.emergency.domain.TypeIncident;

public record IncidentRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(max = 150)
        String titre,

        @NotBlank(message = "La description est obligatoire")
        @Size(max = 500)
        String description,

        @NotNull(message = "Le type est obligatoire")
        TypeIncident type,

        @NotBlank(message = "L'adresse est obligatoire")
        @Size(max = 255)
        String adresse,

        @NotNull(message = "La latitude est obligatoire")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @NotNull(message = "La longitude est obligatoire")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        @Min(value = 0, message = "Nombre de victimes invalide")
        Integer nombreVictimes

) {
}