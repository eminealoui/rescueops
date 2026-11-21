package tn.esprit.rescueops.hospital.dto;

import tn.esprit.rescueops.hospital.domain.Specialite;

import java.util.List;
import java.util.UUID;

/** Vue « Capacités hospitalières » de la maquette : 4 indicateurs + une ligne par service. */
public record TableauDeBordResponse(Kpis kpis, List<LigneService> services) {

    /** @param tauxAcceptation24h en %, ou null si aucune décision sur 24 h */
    public record Kpis(int placesLibres, long reservationsActives, long servicesSatures, Integer tauxAcceptation24h) {
    }

    /** @param etat SATURE (0 place), RESERVE (au moins une place retenue) ou DISPONIBLE */
    public record LigneService(UUID serviceId, UUID hopitalId, String hopitalNom, String ville, Specialite specialite,
                               int capaciteTotale, int placesDisponibles, long placesRetenues, String etat) {
    }
}
