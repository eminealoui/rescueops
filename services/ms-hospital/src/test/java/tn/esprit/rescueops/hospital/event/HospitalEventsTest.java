package tn.esprit.rescueops.hospital.event;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.hospital.domain.DemandeTransfert;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.EvenementTraiteRepository;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;
import tn.esprit.rescueops.hospital.service.TransfertService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HospitalEventsTest {

    @Test
    void confirmationPublieTransfertConfirme() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        HospitalEventPublisher publisher = new HospitalEventPublisher(rabbit, mock(ServiceHospitalierRepository.class));
        UUID t = UUID.randomUUID(), m = UUID.randomUUID();

        publisher.surTransition(new TransfertStatutChange(t, m, null, StatutTransfert.CONFIRMEE));

        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq("transfert.confirme"), message.capture());
        RescueOpsEvent<?> evt = (RescueOpsEvent<?>) message.getValue();
        assertThat(evt.eventType()).isEqualTo("transfert.confirme");
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) evt.payload();
        assertThat(payload).containsEntry("transfertId", t.toString())
                .containsEntry("missionId", m.toString()).containsEntry("statut", "CONFIRMEE");
    }

    @Test
    void lesStatutsInternesNeSontPasPublies() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        HospitalEventPublisher publisher = new HospitalEventPublisher(rabbit, mock(ServiceHospitalierRepository.class));

        publisher.surTransition(new TransfertStatutChange(UUID.randomUUID(), UUID.randomUUID(), null, StatutTransfert.DEMANDEE));

        verifyNoInteractions(rabbit);
    }

    @Test
    void lePublieurEcouteApresCommitSeulement() throws Exception {
        var annotation = HospitalEventPublisher.class.getMethod("surTransition", TransfertStatutChange.class)
                .getAnnotation(org.springframework.transaction.event.TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT);
    }

    @Test
    void missionTermineeAnnuleLesDemandesEnCours() {
        DemandeTransfertRepository demandes = mock(DemandeTransfertRepository.class);
        EvenementTraiteRepository traites = mock(EvenementTraiteRepository.class);
        TransfertService transferts = mock(TransfertService.class);
        UUID mission = UUID.randomUUID();
        DemandeTransfert a = demande(StatutTransfert.PLACE_RETENUE), b = demande(StatutTransfert.DEMANDEE);
        when(demandes.findByMissionIdAndStatutIn(eq(mission), anyList())).thenReturn(List.of(a, b));

        new MissionTermineeListener(demandes, traites, transferts)
                .recevoir(new RescueOpsEvent<>(UUID.randomUUID(), "mission.terminee", 1, Instant.now(), null,
                        Map.of("missionId", mission.toString())));

        verify(transferts).annulerAutomatiquement(a.getId(), StatutTransfert.PLACE_RETENUE);
        verify(transferts).annulerAutomatiquement(b.getId(), StatutTransfert.DEMANDEE);
    }

    @Test
    void memeEventIdTraiteUneSeuleFois() {
        DemandeTransfertRepository demandes = mock(DemandeTransfertRepository.class);
        EvenementTraiteRepository traites = mock(EvenementTraiteRepository.class);
        TransfertService transferts = mock(TransfertService.class);
        when(traites.existsById(any())).thenReturn(true);   // déjà traité lors d'une livraison précédente

        new MissionTermineeListener(demandes, traites, transferts)
                .recevoir(new RescueOpsEvent<>(UUID.randomUUID(), "mission.terminee", 1, Instant.now(), null,
                        Map.of("missionId", UUID.randomUUID().toString())));

        verifyNoInteractions(transferts);
        verify(demandes, never()).findByMissionIdAndStatutIn(any(), any());
    }

    @Test
    void messageSansMissionIdEstIgnoreSansErreur() {
        DemandeTransfertRepository demandes = mock(DemandeTransfertRepository.class);
        TransfertService transferts = mock(TransfertService.class);

        new MissionTermineeListener(demandes, mock(EvenementTraiteRepository.class), transferts)
                .recevoir(new RescueOpsEvent<>(UUID.randomUUID(), "mission.terminee", 1, Instant.now(), null, Map.of()));

        verifyNoInteractions(transferts);
    }

    private static DemandeTransfert demande(StatutTransfert statut) {
        DemandeTransfert d = new DemandeTransfert();
        d.setId(UUID.randomUUID());
        d.setStatut(statut);
        return d;
    }

    @SuppressWarnings("unused")
    private static Optional<Object> unused() {
        return Optional.empty();
    }
}
