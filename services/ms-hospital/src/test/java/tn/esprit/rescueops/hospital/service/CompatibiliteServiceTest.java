package tn.esprit.rescueops.hospital.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.esprit.rescueops.hospital.config.HospitalProperties;
import tn.esprit.rescueops.hospital.domain.Gravite;
import tn.esprit.rescueops.hospital.domain.Hopital;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;
import tn.esprit.rescueops.hospital.domain.Specialite;
import tn.esprit.rescueops.hospital.dto.CandidatDto;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CompatibiliteServiceTest {

    // Origine : Tunis centre. ~0,03° de latitude ≈ 3,3 km ; ~0,27° ≈ 30 km.
    static final double LAT = 36.8000, LON = 10.1800;

    private ServiceHospitalierRepository repo;
    private CompatibiliteService service;
    private final List<ServiceHospitalier> actifs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        repo = mock(ServiceHospitalierRepository.class);
        when(repo.findActifsAvecHopital()).thenReturn(actifs);
        service = new CompatibiliteService(repo, new HospitalProperties(Duration.ofMinutes(15), Duration.ofSeconds(30), 50));
    }

    private ServiceHospitalier ajouter(String nom, double lat, Specialite sp, int libres, int capacite) {
        Hopital h = new Hopital();
        h.setId(UUID.randomUUID());
        h.setNom(nom);
        h.setLatitude(lat);
        h.setLongitude(LON);
        h.setActif(true);
        ServiceHospitalier s = new ServiceHospitalier();
        s.setId(UUID.randomUUID());
        s.setHopital(h);
        s.setSpecialite(sp);
        s.setPlacesDisponibles(libres);
        s.setCapaciteTotale(capacite);
        s.setActif(true);
        actifs.add(s);
        return s;
    }

    @Test
    void critiquePrivilegieLePlusProche() {
        ajouter("Proche", LAT + 0.03, Specialite.CARDIOLOGIE, 1, 20);
        ajouter("Loin", LAT + 0.27, Specialite.CARDIOLOGIE, 15, 20);

        List<CandidatDto> r = service.proposer(Specialite.CARDIOLOGIE, LAT, LON, Gravite.CRITIQUE);

        assertThat(r).extracting(CandidatDto::hopitalNom).containsExactly("Proche", "Loin");
    }

    @Test
    void faiblePrivilegieLaDisponibilite() {
        ajouter("Proche", LAT + 0.03, Specialite.CARDIOLOGIE, 1, 20);
        ajouter("Loin", LAT + 0.27, Specialite.CARDIOLOGIE, 15, 20);

        List<CandidatDto> r = service.proposer(Specialite.CARDIOLOGIE, LAT, LON, Gravite.FAIBLE);

        assertThat(r).extracting(CandidatDto::hopitalNom).containsExactly("Loin", "Proche");
    }

    @Test
    void repliUrgencesGeneralesMoinsBienNote() {
        ajouter("Exact", LAT + 0.03, Specialite.CARDIOLOGIE, 5, 10);
        ajouter("Repli", LAT + 0.03, Specialite.URGENCES_GENERALES, 5, 10);
        ajouter("AutreSpecialite", LAT + 0.03, Specialite.PEDIATRIE, 5, 10);

        List<CandidatDto> r = service.proposer(Specialite.CARDIOLOGIE, LAT, LON, Gravite.ELEVEE);

        assertThat(r).extracting(CandidatDto::hopitalNom).containsExactly("Exact", "Repli");
        assertThat(r.get(1).sousScores().specialite()).isEqualTo(40);
    }

    @Test
    void horsRayonExclu() {
        ajouter("TresLoin", LAT + 0.60, Specialite.CARDIOLOGIE, 5, 10);   // ~67 km > 50 km

        assertThat(service.proposer(Specialite.CARDIOLOGIE, LAT, LON, Gravite.MOYENNE)).isEmpty();
    }

    @Test
    void sansPlaceExclu() {
        ajouter("Complet", LAT + 0.03, Specialite.CARDIOLOGIE, 0, 10);

        assertThat(service.proposer(Specialite.CARDIOLOGIE, LAT, LON, Gravite.MOYENNE)).isEmpty();
    }

    @Test
    void sansCoordonneesDistanceInconnue() {
        ajouter("A", LAT + 0.60, Specialite.CARDIOLOGIE, 5, 10);

        List<CandidatDto> r = service.proposer(Specialite.CARDIOLOGIE, null, null, Gravite.MOYENNE);

        assertThat(r).hasSize(1);
        assertThat(r.get(0).distanceKm()).isNull();
        assertThat(r.get(0).sousScores().distance()).isZero();
        assertThat(r.get(0).justification()).contains("distance inconnue");
    }

    @Test
    void justificationLisibleEtScoreBorne() {
        ajouter("A", LAT + 0.03, Specialite.CARDIOLOGIE, 3, 10);

        CandidatDto c = service.proposer(Specialite.CARDIOLOGIE, LAT, LON, Gravite.CRITIQUE).get(0);

        assertThat(c.justification()).contains("km").contains("30 % de places libres").contains("spécialité exacte");
        assertThat(c.score()).isBetween(0, 100);
        assertThat(c.distanceKm()).isBetween(3.0, 3.6);
    }
}
