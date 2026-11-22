package tn.esprit.rescueops.intervention.dto;

import tn.esprit.rescueops.intervention.domain.StatutReaffectation;

import java.time.Instant;
import java.util.UUID;

public record DemandeReaffectationResponse(
    UUID id,
    UUID missionId,
    String motif,
    StatutReaffectation statut,
    String demandePar,
    Instant dateDemande,
    Instant dateTraitement
) {
}
