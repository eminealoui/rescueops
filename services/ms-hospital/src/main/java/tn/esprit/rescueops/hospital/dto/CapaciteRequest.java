package tn.esprit.rescueops.hospital.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ajustement manuel par le coordinateur : nouvelle capacité et places réellement libres
 * (ex. patients arrivés hors RescueOps). Tracé dans historique_capacite.
 */
public record CapaciteRequest(@NotNull @Min(0) @Max(2000) Integer capaciteTotale,
                              @NotNull @Min(0) Integer placesDisponibles,
                              @Size(max = 500) String motif) {
}
