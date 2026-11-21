package tn.esprit.rescueops.hospital.dto;

import tn.esprit.rescueops.hospital.domain.Gravite;
import tn.esprit.rescueops.hospital.domain.Specialite;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Demande de transfert. {@code historique} n'est rempli que sur le détail (GET /api/transfers/{id}). */
public record TransfertResponse(UUID id, UUID missionId, Specialite specialiteRequise, Gravite gravite,
                                Double latitudeOrigine, Double longitudeOrigine, UUID serviceId, String hopitalNom,
                                StatutTransfert statut, Instant expireAt, String demandeurUserId,
                                String traiteParUserId, String motifRefus, Instant createdAt,
                                List<Etape> historique) {

    public record Etape(StatutTransfert ancienStatut, StatutTransfert nouveauStatut, String acteurUserId,
                        Instant horodatage, String commentaire) {
    }
}
