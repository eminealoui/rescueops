package tn.esprit.rescueops.hospital.dto;

import jakarta.validation.constraints.NotNull;

/** Activer ou fermer un service (un service fermé n'est plus proposé ni réservable). */
public record ServiceActifRequest(@NotNull Boolean actif) {
}
