package tn.esprit.rescueops.hospital.dto;

import tn.esprit.rescueops.hospital.domain.Specialite;

import java.util.UUID;

/**
 * Établissement proposé pour un transfert, avec un score sur 100 et sa justification (recommandation explicable).
 *
 * @param distanceKm null si la position d'origine est inconnue
 */
public record CandidatDto(UUID serviceId, UUID hopitalId, String hopitalNom, Specialite specialite,
                          int placesDisponibles, int capaciteTotale, Double distanceKm, int score,
                          SousScores sousScores, String justification) {

    /** Chaque critère noté de 0 à 100, avant pondération. */
    public record SousScores(int distance, int disponibilite, int specialite) {
    }
}
