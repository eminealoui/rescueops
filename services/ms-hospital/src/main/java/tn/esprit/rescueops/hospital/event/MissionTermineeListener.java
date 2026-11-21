package tn.esprit.rescueops.hospital.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.hospital.domain.DemandeTransfert;
import tn.esprit.rescueops.hospital.domain.EvenementTraite;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.EvenementTraiteRepository;
import tn.esprit.rescueops.hospital.service.TransfertService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fin de mission (publiée par ms-intervention) : les demandes encore ouvertes de cette mission sont annulées
 * et leurs places rendues.
 * Idempotence (BNF-08) : l'eventId est enregistré DANS LA MÊME transaction que les annulations.
 * Deux livraisons simultanées : la clé primaire en fait échouer une, qui est annulée en bloc puis rejouée,
 * et le rejeu voit que l'événement est déjà traité.
 */
@Component
public class MissionTermineeListener {

    private static final Logger log = LoggerFactory.getLogger(MissionTermineeListener.class);

    private final DemandeTransfertRepository demandes;
    private final EvenementTraiteRepository traites;
    private final TransfertService transferts;

    public MissionTermineeListener(DemandeTransfertRepository demandes, EvenementTraiteRepository traites,
                                   TransfertService transferts) {
        this.demandes = demandes;
        this.traites = traites;
        this.transferts = transferts;
    }

    @RabbitListener(queues = MessagingConfig.FILE_MISSION_TERMINEE)
    @Transactional
    public void recevoir(RescueOpsEvent<Map<String, Object>> event) {
        if (event.eventId() == null || traites.existsById(event.eventId())) {
            return;
        }
        Object missionId = event.payload() == null ? null : event.payload().get("missionId");
        if (missionId == null) {
            log.warn("mission.terminee sans missionId ignoré (eventId {})", event.eventId());
            traites.save(new EvenementTraite(event.eventId(), Instant.now()));
            return;
        }
        List<DemandeTransfert> ouvertes = demandes.findByMissionIdAndStatutIn(UUID.fromString(missionId.toString()),
                List.of(StatutTransfert.DEMANDEE, StatutTransfert.PLACE_RETENUE));
        for (DemandeTransfert d : ouvertes) {
            transferts.annulerAutomatiquement(d.getId(), d.getStatut());
        }
        traites.save(new EvenementTraite(event.eventId(), Instant.now()));
        log.info("mission.terminee {} : {} demande(s) annulée(s)", missionId, ouvertes.size());
    }
}
