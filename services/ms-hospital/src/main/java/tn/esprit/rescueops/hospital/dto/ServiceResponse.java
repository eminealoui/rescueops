package tn.esprit.rescueops.hospital.dto;

import tn.esprit.rescueops.hospital.domain.Specialite;

import java.util.UUID;

public record ServiceResponse(UUID id, UUID hopitalId, Specialite specialite, int capaciteTotale,
                              int placesDisponibles, boolean actif) {
}
