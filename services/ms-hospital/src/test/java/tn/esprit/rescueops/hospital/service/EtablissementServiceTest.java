package tn.esprit.rescueops.hospital.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.hospital.domain.HistoriqueCapacite;
import tn.esprit.rescueops.hospital.domain.Hopital;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;
import tn.esprit.rescueops.hospital.domain.Specialite;
import tn.esprit.rescueops.hospital.dto.CapaciteRequest;
import tn.esprit.rescueops.hospital.mapper.HospitalMapperImpl;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.HistoriqueCapaciteRepository;
import tn.esprit.rescueops.hospital.repository.HopitalRepository;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EtablissementServiceTest {

    private HopitalRepository hopitaux;
    private ServiceHospitalierRepository services;
    private HistoriqueCapaciteRepository historique;
    private DemandeTransfertRepository demandes;
    private EtablissementService service;
    private final UUID serviceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        hopitaux = mock(HopitalRepository.class);
        services = mock(ServiceHospitalierRepository.class);
        historique = mock(HistoriqueCapaciteRepository.class);
        demandes = mock(DemandeTransfertRepository.class);
        service = new EtablissementService(hopitaux, services, historique, demandes, new HospitalMapperImpl(),
                mock(ApplicationEventPublisher.class));
    }

    /** Service dont « occupeesParRescueOps » places sont tenues par des demandes RETENUE / CONFIRMEE / ADMIS. */
    private void serviceExistant(int capacite, int libres) {
        serviceExistant(capacite, libres, capacite - libres);
    }

    private void serviceExistant(int capacite, int libres, long occupeesParRescueOps) {
        Hopital h = new Hopital();
        h.setId(UUID.randomUUID());
        ServiceHospitalier s = new ServiceHospitalier();
        s.setId(serviceId);
        s.setHopital(h);
        s.setSpecialite(Specialite.CARDIOLOGIE);
        s.setCapaciteTotale(capacite);
        s.setPlacesDisponibles(libres);
        s.setActif(true);
        when(services.findById(serviceId)).thenReturn(Optional.of(s));
        when(services.findByIdPourMiseAJour(serviceId)).thenReturn(Optional.of(s));
        when(demandes.compterPlacesOccupees(serviceId)).thenReturn(occupeesParRescueOps);
    }

    @Test
    void ajusterCapaciteSousLesPlacesOccupees422() {
        serviceExistant(10, 4);   // 6 places occupées

        assertThatThrownBy(() -> service.ajusterCapacite(serviceId, new CapaciteRequest(5, 0, "travaux"), "coord"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("CAPACITE_INVALIDE");
        verify(services, never()).ajusterCapacite(any(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void ajusterCapacitePlacesHorsBornes422() {
        serviceExistant(10, 4);

        assertThatThrownBy(() -> service.ajusterCapacite(serviceId, new CapaciteRequest(10, 11, null), "coord"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("CAPACITE_INVALIDE");
    }

    @Test
    void ajusterCapaciteEcritLHistorique() {
        serviceExistant(10, 4);
        when(services.ajusterCapacite(serviceId, 10, 4, 12, 6)).thenReturn(1);

        service.ajusterCapacite(serviceId, new CapaciteRequest(12, 6, "nouvelle aile"), "coord");

        ArgumentCaptor<HistoriqueCapacite> h = ArgumentCaptor.forClass(HistoriqueCapacite.class);
        verify(historique).save(h.capture());
        assertThat(h.getValue().getAncienneCapacite()).isEqualTo(10);
        assertThat(h.getValue().getNouvelleCapacite()).isEqualTo(12);
        assertThat(h.getValue().getNouvellesPlaces()).isEqualTo(6);
        assertThat(h.getValue().getActeurUserId()).isEqualTo("coord");
        assertThat(h.getValue().getMotif()).isEqualTo("nouvelle aile");
    }

    @Test
    void ajusterCapacitePendantUneReservation409() {
        serviceExistant(10, 4);
        when(services.ajusterCapacite(serviceId, 10, 4, 12, 6)).thenReturn(0);   // une réservation est passée entre-temps

        assertThatThrownBy(() -> service.ajusterCapacite(serviceId, new CapaciteRequest(12, 6, null), "coord"))
                .isInstanceOf(ConflictException.class)
                .extracting("code").isEqualTo("MODIFICATION_CONCURRENTE");
        verify(historique, never()).save(any());
    }

    /** Revue C2 : 8 lits tenus par des transferts, le coordinateur ne peut pas déclarer 10 places libres sur 10. */
    @Test
    void ajusterCapaciteNePeutPasLibererDesLitsTenusParDesTransferts() {
        serviceExistant(10, 2, 8);

        assertThatThrownBy(() -> service.ajusterCapacite(serviceId, new CapaciteRequest(10, 10, "erreur"), "coord"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code").isEqualTo("CAPACITE_INVALIDE");
        verify(services, never()).ajusterCapacite(any(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    /** Patients sortis hors RescueOps : le coordinateur peut libérer les lits qui ne sont PAS tenus par un transfert. */
    @Test
    void ajusterCapacitePeutLibererLesLitsHorsRescueOps() {
        serviceExistant(10, 2, 3);   // 8 occupés, dont seulement 3 par des transferts RescueOps
        when(services.ajusterCapacite(serviceId, 10, 2, 10, 7)).thenReturn(1);

        service.ajusterCapacite(serviceId, new CapaciteRequest(10, 7, "sorties hors RescueOps"), "coord");

        verify(services).ajusterCapacite(serviceId, 10, 2, 10, 7);
    }

    /** Revue C1 : fermer un service ne doit jamais réécrire le compteur de places (requête ciblée). */
    @Test
    void fermerUnServiceNeTouchePasAuxPlaces() {
        serviceExistant(10, 4);
        when(services.changerActif(serviceId, false)).thenReturn(1);

        service.changerEtatService(serviceId, false);

        verify(services).changerActif(serviceId, false);
        verify(services, never()).save(any());
    }

    @Test
    void supprimerHopitalEstLogique() {
        Hopital h = new Hopital();
        h.setId(UUID.randomUUID());
        h.setActif(true);
        when(hopitaux.findById(h.getId())).thenReturn(Optional.of(h));

        service.supprimerHopital(h.getId());

        assertThat(h.isActif()).isFalse();
        verify(hopitaux, never()).delete(any());
        verify(hopitaux, never()).deleteById(any());
    }
}
