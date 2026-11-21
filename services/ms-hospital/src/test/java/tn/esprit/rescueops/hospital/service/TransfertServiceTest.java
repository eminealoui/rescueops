package tn.esprit.rescueops.hospital.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.ForbiddenOperationException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.hospital.config.HospitalProperties;
import tn.esprit.rescueops.hospital.domain.*;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.HistoriqueTransfertRepository;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TransfertServiceTest {

    private DemandeTransfertRepository demandes;
    private ServiceHospitalierRepository services;
    private HistoriqueTransfertRepository historique;
    private TransfertService service;

    private final UUID demandeId = UUID.randomUUID();
    private final UUID serviceId = UUID.randomUUID();

    private static final CurrentUser AMBULANCIER = new CurrentUser("amb-1", null, null, null, Set.of(Role.AMBULANCIER));
    private static final CurrentUser AUTRE_AMBULANCIER = new CurrentUser("amb-2", null, null, null, Set.of(Role.AMBULANCIER));
    private static final CurrentUser COORDINATEUR = new CurrentUser("coord", null, null, null, Set.of(Role.COORDINATEUR_HOSPITALIER));

    @BeforeEach
    void setUp() {
        demandes = mock(DemandeTransfertRepository.class);
        services = mock(ServiceHospitalierRepository.class);
        historique = mock(HistoriqueTransfertRepository.class);
        HospitalProperties props = new HospitalProperties(Duration.ofMinutes(15), Duration.ofSeconds(30), 50);
        service = new TransfertService(demandes, services, historique, mock(ApplicationEventPublisher.class), props);
    }

    private DemandeTransfert demande(StatutTransfert statut, boolean avecService) {
        DemandeTransfert d = new DemandeTransfert();
        d.setId(demandeId);
        d.setMissionId(UUID.randomUUID());
        d.setStatut(statut);
        d.setDemandeurUserId("amb-1");
        d.setSpecialiteRequise(Specialite.CARDIOLOGIE);
        d.setGravite(Gravite.CRITIQUE);
        if (avecService) {
            ServiceHospitalier s = new ServiceHospitalier();
            s.setId(serviceId);
            d.setService(s);
        }
        when(demandes.findById(demandeId)).thenReturn(Optional.of(d));
        return d;
    }

    @Test
    void reserverSansPlace409CapaciteEpuisee() {
        demande(StatutTransfert.DEMANDEE, false);
        when(demandes.retenirPlace(eq(demandeId), eq(serviceId), any(), any(), any())).thenReturn(1);
        when(services.reserverPlace(serviceId)).thenReturn(0);

        assertThatThrownBy(() -> service.reserver(demandeId, serviceId, AMBULANCIER))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("CAPACITE_EPUISEE");
    }

    @Test
    void reserverUneDemandeDejaTraitee409() {
        demande(StatutTransfert.CONFIRMEE, true);
        when(demandes.retenirPlace(any(), any(), any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.reserver(demandeId, serviceId, AMBULANCIER))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("TRANSITION_INTERDITE");
        verify(services, never()).reserverPlace(any());
    }

    @Test
    void confirmerDepuisDemandee409() {
        demande(StatutTransfert.DEMANDEE, false);
        when(demandes.changerStatut(demandeId, StatutTransfert.PLACE_RETENUE, StatutTransfert.CONFIRMEE, "coord", null))
                .thenReturn(0);

        assertThatThrownBy(() -> service.confirmer(demandeId, COORDINATEUR))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("TRANSITION_INTERDITE");
    }

    @Test
    void refuserRendLaPlace() {
        demande(StatutTransfert.PLACE_RETENUE, true);
        when(demandes.changerStatut(demandeId, StatutTransfert.PLACE_RETENUE, StatutTransfert.REFUSEE, "coord", "complet"))
                .thenReturn(1);

        service.refuser(demandeId, "complet", COORDINATEUR);

        verify(services).rendrePlace(serviceId);
    }

    @Test
    void sortieRendLaPlace() {
        demande(StatutTransfert.ADMIS, true);
        when(demandes.changerStatut(demandeId, StatutTransfert.ADMIS, StatutTransfert.SORTI, "coord", null)).thenReturn(1);

        service.sortie(demandeId, COORDINATEUR);

        verify(services).rendrePlace(serviceId);
    }

    @Test
    void annulerConfirmeeRendLaPlace() {
        demande(StatutTransfert.CONFIRMEE, true);
        when(demandes.changerStatut(demandeId, StatutTransfert.CONFIRMEE, StatutTransfert.ANNULEE, "amb-1", null)).thenReturn(1);

        service.annuler(demandeId, AMBULANCIER);

        verify(services).rendrePlace(serviceId);
    }

    @Test
    void annulerDemandeeNeRendRien() {
        demande(StatutTransfert.DEMANDEE, false);
        when(demandes.changerStatut(demandeId, StatutTransfert.DEMANDEE, StatutTransfert.ANNULEE, "amb-1", null)).thenReturn(1);

        service.annuler(demandeId, AMBULANCIER);

        verify(services, never()).rendrePlace(any());
    }

    @Test
    void annulerAdmisInterdit409() {
        demande(StatutTransfert.ADMIS, true);

        assertThatThrownBy(() -> service.annuler(demandeId, COORDINATEUR))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("TRANSITION_INTERDITE");
        verify(services, never()).rendrePlace(any());
    }

    @Test
    void annulerParUnAutreAmbulancier403() {
        demande(StatutTransfert.DEMANDEE, false);

        assertThatThrownBy(() -> service.annuler(demandeId, AUTRE_AMBULANCIER))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(demandes, never()).changerStatut(any(), any(), any(), any(), any());
    }

    @Test
    void chaqueTransitionEcritLHistorique() {
        demande(StatutTransfert.PLACE_RETENUE, true);
        when(demandes.changerStatut(demandeId, StatutTransfert.PLACE_RETENUE, StatutTransfert.CONFIRMEE, "coord", null))
                .thenReturn(1);

        service.confirmer(demandeId, COORDINATEUR);

        ArgumentCaptor<HistoriqueTransfert> h = ArgumentCaptor.forClass(HistoriqueTransfert.class);
        verify(historique).save(h.capture());
        assertThat(h.getValue().getAncienStatut()).isEqualTo(StatutTransfert.PLACE_RETENUE);
        assertThat(h.getValue().getNouveauStatut()).isEqualTo(StatutTransfert.CONFIRMEE);
        assertThat(h.getValue().getActeurUserId()).isEqualTo("coord");
    }
}
