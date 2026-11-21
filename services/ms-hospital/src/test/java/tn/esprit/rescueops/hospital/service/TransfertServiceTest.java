package tn.esprit.rescueops.hospital.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.ForbiddenOperationException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.hospital.config.HospitalProperties;
import tn.esprit.rescueops.hospital.domain.*;
import tn.esprit.rescueops.hospital.dto.CreerTransfertRequest;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.HistoriqueTransfertRepository;
import tn.esprit.rescueops.hospital.repository.MissionTermineeRepository;
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
    private MissionTermineeRepository missionsTerminees;
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
        missionsTerminees = mock(MissionTermineeRepository.class);
        HospitalProperties props = new HospitalProperties(Duration.ofMinutes(15), Duration.ofSeconds(30), 50);
        service = new TransfertService(demandes, services, historique, missionsTerminees,
                mock(ApplicationEventPublisher.class), props);
        when(demandes.save(any())).thenAnswer(i -> {
            DemandeTransfert d = i.getArgument(0);
            d.setId(demandeId);
            return d;
        });
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

    private void serviceCible(Specialite specialite) {
        ServiceHospitalier s = new ServiceHospitalier();
        s.setId(serviceId);
        s.setSpecialite(specialite);
        when(services.findById(serviceId)).thenReturn(Optional.of(s));
    }

    private static CreerTransfertRequest requete(UUID mission) {
        return new CreerTransfertRequest(mission, Specialite.CARDIOLOGIE, Gravite.CRITIQUE, 36.8, 10.18);
    }

    // ---------- création ----------

    @Test
    void creerPourUneMissionTermineeEstRefuse() {
        UUID mission = UUID.randomUUID();
        when(missionsTerminees.existsById(mission)).thenReturn(true);

        assertThatThrownBy(() -> service.creer(requete(mission), AMBULANCIER))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("MISSION_TERMINEE");
        verify(demandes, never()).save(any());
    }

    @Test
    void uneSeuleDemandeOuverteParMission() {
        UUID mission = UUID.randomUUID();
        when(demandes.existsByMissionIdAndStatutIn(eq(mission), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> service.creer(requete(mission), AMBULANCIER))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("DEMANDE_EN_COURS");
    }

    // ---------- réservation ----------

    @Test
    void reserverSansPlace409CapaciteEpuisee() {
        demande(StatutTransfert.DEMANDEE, false);
        serviceCible(Specialite.CARDIOLOGIE);
        when(services.reserverPlace(serviceId)).thenReturn(0);

        assertThatThrownBy(() -> service.reserver(demandeId, serviceId, AMBULANCIER))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("CAPACITE_EPUISEE");
    }

    @Test
    void reserverUneDemandeDejaTraitee409() {
        demande(StatutTransfert.CONFIRMEE, true);
        serviceCible(Specialite.CARDIOLOGIE);

        assertThatThrownBy(() -> service.reserver(demandeId, serviceId, AMBULANCIER))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("TRANSITION_INTERDITE");
        verify(services, never()).reserverPlace(any());
    }

    @Test
    void reserverDansUnServiceInconnu404() {
        demande(StatutTransfert.DEMANDEE, false);

        assertThatThrownBy(() -> service.reserver(demandeId, serviceId, AMBULANCIER)).isInstanceOf(NotFoundException.class);
        verify(services, never()).reserverPlace(any());
    }

    @Test
    void reserverDansUneSpecialiteIncompatible422() {
        demande(StatutTransfert.DEMANDEE, false);
        serviceCible(Specialite.PEDIATRIE);

        assertThatThrownBy(() -> service.reserver(demandeId, serviceId, AMBULANCIER))
                .isInstanceOf(BusinessRuleException.class).extracting("code").isEqualTo("SERVICE_INCOMPATIBLE");
        verify(services, never()).reserverPlace(any());
    }

    @Test
    void reserverEnRepliUrgencesGeneralesEstAccepte() {
        demande(StatutTransfert.DEMANDEE, false);
        serviceCible(Specialite.URGENCES_GENERALES);
        when(services.reserverPlace(serviceId)).thenReturn(1);
        when(demandes.retenirPlace(eq(demandeId), eq(serviceId), any(), any(), any())).thenReturn(1);

        service.reserver(demandeId, serviceId, AMBULANCIER);

        verify(services).reserverPlace(serviceId);
    }

    @Test
    void reserverLaDemandeDUnAutreAmbulancier403() {
        demande(StatutTransfert.DEMANDEE, false);
        serviceCible(Specialite.CARDIOLOGIE);

        assertThatThrownBy(() -> service.reserver(demandeId, serviceId, AUTRE_AMBULANCIER))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(services, never()).reserverPlace(any());
    }

    // ---------- transitions ----------

    @Test
    void confirmerUneReservationExpiree409() {
        demande(StatutTransfert.PLACE_RETENUE, true);
        when(demandes.confirmer(eq(demandeId), eq("coord"), any())).thenReturn(0);

        assertThatThrownBy(() -> service.confirmer(demandeId, COORDINATEUR))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("RESERVATION_EXPIREE");
    }

    @Test
    void confirmerDepuisDemandee409() {
        demande(StatutTransfert.DEMANDEE, false);

        assertThatThrownBy(() -> service.confirmer(demandeId, COORDINATEUR))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("TRANSITION_INTERDITE");
        verify(demandes, never()).confirmer(any(), any(), any());
    }

    @Test
    void refuserRendLaPlace() {
        demande(StatutTransfert.PLACE_RETENUE, true);
        when(demandes.changerStatut(eq(demandeId), eq(StatutTransfert.PLACE_RETENUE), eq(StatutTransfert.REFUSEE),
                eq("coord"), eq("complet"), any())).thenReturn(1);
        when(services.rendrePlace(serviceId)).thenReturn(1);

        service.refuser(demandeId, "complet", COORDINATEUR);

        verify(services).rendrePlace(serviceId);
    }

    @Test
    void sortieRendLaPlace() {
        demande(StatutTransfert.ADMIS, true);
        when(demandes.changerStatut(eq(demandeId), eq(StatutTransfert.ADMIS), eq(StatutTransfert.SORTI), eq("coord"),
                isNull(), any())).thenReturn(1);
        when(services.rendrePlace(serviceId)).thenReturn(1);

        service.sortie(demandeId, COORDINATEUR);

        verify(services).rendrePlace(serviceId);
    }

    @Test
    void annulerConfirmeeRendLaPlace() {
        demande(StatutTransfert.CONFIRMEE, true);
        when(demandes.changerStatut(eq(demandeId), eq(StatutTransfert.CONFIRMEE), eq(StatutTransfert.ANNULEE),
                eq("amb-1"), isNull(), any())).thenReturn(1);
        when(services.rendrePlace(serviceId)).thenReturn(1);

        service.annuler(demandeId, AMBULANCIER);

        verify(services).rendrePlace(serviceId);
    }

    @Test
    void annulerDemandeeNeRendRien() {
        demande(StatutTransfert.DEMANDEE, false);
        when(demandes.changerStatut(eq(demandeId), eq(StatutTransfert.DEMANDEE), eq(StatutTransfert.ANNULEE),
                eq("amb-1"), isNull(), any())).thenReturn(1);

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
        verify(demandes, never()).changerStatut(any(), any(), any(), any(), any(), any());
    }

    @Test
    void annulationAutomatiqueRelitLeStatutCourant() {
        // La liste de la mission voyait DEMANDEE, mais une réservation est passée entre-temps : la place doit être rendue.
        demande(StatutTransfert.PLACE_RETENUE, true);
        when(demandes.changerStatut(eq(demandeId), eq(StatutTransfert.PLACE_RETENUE), eq(StatutTransfert.ANNULEE),
                eq(TransfertService.SYSTEME), isNull(), any())).thenReturn(1);
        when(services.rendrePlace(serviceId)).thenReturn(1);

        assertThat(service.annulerAutomatiquement(demandeId)).isTrue();
        verify(services).rendrePlace(serviceId);
    }

    @Test
    void chaqueTransitionEcritLHistorique() {
        demande(StatutTransfert.PLACE_RETENUE, true);
        when(demandes.confirmer(eq(demandeId), eq("coord"), any())).thenReturn(1);

        service.confirmer(demandeId, COORDINATEUR);

        ArgumentCaptor<HistoriqueTransfert> h = ArgumentCaptor.forClass(HistoriqueTransfert.class);
        verify(historique).save(h.capture());
        assertThat(h.getValue().getAncienStatut()).isEqualTo(StatutTransfert.PLACE_RETENUE);
        assertThat(h.getValue().getNouveauStatut()).isEqualTo(StatutTransfert.CONFIRMEE);
        assertThat(h.getValue().getActeurUserId()).isEqualTo("coord");
    }
}
