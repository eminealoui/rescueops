package tn.esprit.rescueops.hospital.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;
import tn.esprit.rescueops.hospital.domain.Specialite;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.dto.TableauDeBordResponse;
import tn.esprit.rescueops.hospital.dto.TableauDeBordResponse.Kpis;
import tn.esprit.rescueops.hospital.dto.TableauDeBordResponse.LigneService;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.HistoriqueTransfertRepository;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Indicateurs et liste des services pour l'écran « Capacités hospitalières ». */
@Service
public class TableauDeBordService {

    private final ServiceHospitalierRepository services;
    private final DemandeTransfertRepository demandes;
    private final HistoriqueTransfertRepository historique;

    public TableauDeBordService(ServiceHospitalierRepository services, DemandeTransfertRepository demandes,
                                HistoriqueTransfertRepository historique) {
        this.services = services;
        this.demandes = demandes;
        this.historique = historique;
    }

    @Transactional(readOnly = true)
    public TableauDeBordResponse tableauDeBord(String ville, Specialite specialite, String etat) {
        Map<UUID, Long> retenues = new HashMap<>();
        for (Object[] ligne : demandes.reservationsParService()) {
            retenues.put((UUID) ligne[0], (Long) ligne[1]);
        }
        List<LigneService> lignes = services.findActifsAvecHopital().stream()
                .map(s -> ligne(s, retenues.getOrDefault(s.getId(), 0L)))
                .filter(l -> ville == null || l.ville().equalsIgnoreCase(ville))
                .filter(l -> specialite == null || l.specialite() == specialite)
                .filter(l -> etat == null || l.etat().equalsIgnoreCase(etat))
                .sorted(Comparator.comparing(LigneService::hopitalNom).thenComparing(LigneService::specialite))
                .toList();

        Instant depuis = Instant.now().minus(Duration.ofHours(24));
        long acceptes = historique.compterDepuis(List.of(StatutTransfert.CONFIRMEE), depuis);
        long decisions = historique.compterDepuis(
                List.of(StatutTransfert.CONFIRMEE, StatutTransfert.REFUSEE, StatutTransfert.EXPIREE), depuis);
        Kpis kpis = new Kpis(
                lignes.stream().mapToInt(LigneService::placesDisponibles).sum(),
                lignes.stream().mapToLong(LigneService::placesRetenues).sum(),
                lignes.stream().filter(l -> l.placesDisponibles() == 0).count(),
                decisions == 0 ? null : (int) Math.round(100.0 * acceptes / decisions));
        return new TableauDeBordResponse(kpis, lignes);
    }

    private static LigneService ligne(ServiceHospitalier s, long retenues) {
        String etat = s.getPlacesDisponibles() == 0 ? "SATURE" : retenues > 0 ? "RESERVE" : "DISPONIBLE";
        return new LigneService(s.getId(), s.getHopital().getId(), s.getHopital().getNom(), s.getHopital().getVille(),
                s.getSpecialite(), s.getCapaciteTotale(), s.getPlacesDisponibles(), retenues, etat);
    }
}
