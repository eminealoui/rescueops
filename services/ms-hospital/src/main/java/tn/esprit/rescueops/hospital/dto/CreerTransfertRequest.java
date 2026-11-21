package tn.esprit.rescueops.hospital.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import tn.esprit.rescueops.hospital.domain.Gravite;
import tn.esprit.rescueops.hospital.domain.Specialite;

import java.util.UUID;

/** Position d'origine facultative : sans elle, la distance n'entre pas dans le score. */
public record CreerTransfertRequest(
        @NotNull UUID missionId,
        @NotNull Specialite specialiteRequise,
        @NotNull Gravite gravite,
        @DecimalMin("-90.0") @DecimalMax("90.0") Double latitudeOrigine,
        @DecimalMin("-180.0") @DecimalMax("180.0") Double longitudeOrigine) {
}
