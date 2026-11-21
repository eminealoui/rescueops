package tn.esprit.rescueops.hospital.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.hospital.domain.DemandeTransfert;
import tn.esprit.rescueops.hospital.domain.HistoriqueTransfert;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.HistoriqueTransfertRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Ticker « EN DIRECT » : dernières transitions, SANS donnée personnelle (ni demandeur, ni patient). */
@Service
public class FluxService {

    public record Evenement(UUID transfertId, StatutTransfert statut, String hopitalNom, Instant horodatage) {
    }

    private final HistoriqueTransfertRepository historique;
    private final DemandeTransfertRepository demandes;

    public FluxService(HistoriqueTransfertRepository historique, DemandeTransfertRepository demandes) {
        this.historique = historique;
        this.demandes = demandes;
    }

    @Transactional(readOnly = true)
    public List<Evenement> derniers(Instant depuis) {
        Instant borne = depuis != null ? depuis : Instant.now().minus(Duration.ofHours(24));
        return historique.findByHorodatageAfterOrderByHorodatageDesc(borne, PageRequest.of(0, 20)).stream()
                .map(this::versEvenement)
                .toList();
    }

    private Evenement versEvenement(HistoriqueTransfert h) {
        String hopital = demandes.findById(h.getDemandeId())
                .map(DemandeTransfert::getService)
                .map(s -> s.getHopital().getNom())
                .orElse(null);
        return new Evenement(h.getDemandeId(), h.getNouveauStatut(), hopital, h.getHorodatage());
    }
}
