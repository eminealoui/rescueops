package tn.esprit.rescueops.hospital.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import tn.esprit.rescueops.hospital.domain.Specialite;

/** Création d'un service : il démarre avec toutes ses places libres. */
public record ServiceRequest(@NotNull Specialite specialite, @NotNull @Min(0) @Max(2000) Integer capaciteTotale) {
}
