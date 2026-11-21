package tn.esprit.rescueops.hospital.dto;

import jakarta.validation.constraints.*;

public record HopitalRequest(
        @NotBlank @Size(max = 150) String nom,
        @Size(max = 255) String adresse,
        @NotBlank @Size(max = 100) String ville,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @Pattern(regexp = "^$|^\\+?[0-9 ]{8,20}$") String telephone) {
}
