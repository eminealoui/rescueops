package tn.esprit.rescueops.hospital.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.hospital.domain.DemandeTransfert;
import tn.esprit.rescueops.hospital.domain.EvenementTraite;
import tn.esprit.rescueops.hospital.domain.MissionTerminee;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.EvenementTraiteRepository;
import tn.esprit.rescueops.hospital.repository.MissionTermineeRepository;
import tn.esprit.rescueops.hospital.service.TransfertService;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fin de mission (publiée par ms-intervention) :
 * <ol>
 *   <li>la mission est mémorisée : plus aucune demande ne pourra être créée pour elle ;</li>
 *   <li>chaque demande encore ouverte est annulée dans SA propre transaction (statut relu : une réservation qui
 *       aboutit au même moment est quand même annulée et sa place rendue) ;</li>
 *   <li>l'eventId est enregistré en dernier.</li>
 * </ol>
 * Idempotence (BNF-08) : une nouvelle livraison voit l'eventId et s'arrête ; deux livraisons simultanées
 * annulent au plus une fois chaque demande (transition conditionnelle), puis la seconde bute sur la clé primaire,
 * est rejouée et s'arrête.
 */
@Component
public class MissionTermineeListener {

    private static final Logger log = LoggerFactory.getLogger(MissionTermineeListener.class);
    private static final List<StatutTransfert> ANNULABLES = List.copyOf(StatutTransfert.ANNULABLE);

    private final DemandeTransfertRepository demandes;
    private final EvenementTraiteRepository traites;
    private final MissionTermineeRepository missions;
    private final TransfertService transferts;

    public MissionTermineeListener(DemandeTransfertRepository demandes, EvenementTraiteRepository traites,
                                   MissionTermineeRepository missions, TransfertService transferts) {
        this.demandes = demandes;
        this.traites = traites;
        this.missions = missions;
        this.transferts = transferts;
    }

    @RabbitListener(queues = MessagingConfig.FILE_MISSION_TERMINEE)
    public void recevoir(RescueOpsEvent<Map<String, Object>> event) {
        if (event.eventId() == null) {
            log.warn("mission.terminee sans eventId ignoré : {}", event);
            return;
        }
        if (traites.existsById(event.eventId())) {
            return;
        }
        UUID missionId = lireMissionId(event);
        if (missionId == null) {
            // Message inexploitable : le rejouer 3 fois ne servirait à rien. Journalisé et écarté.
            traites.save(new EvenementTraite(event.eventId(), Instant.now()));
            return;
        }
        if (!missions.existsById(missionId)) {
            missions.save(new MissionTerminee(missionId, Instant.now()));
        }
        List<DemandeTransfert> ouvertes = demandes.findByMissionIdAndStatutIn(missionId, ANNULABLES);
        int annulees = 0;
        for (DemandeTransfert d : ouvertes) {
            if (transferts.annulerAutomatiquement(d.getId())) {
                annulees++;
            }
        }
        traites.save(new EvenementTraite(event.eventId(), Instant.now()));
        log.info("mission.terminee {} : {} demande(s) annulée(s)", missionId, annulees);
    }

    private static UUID lireMissionId(RescueOpsEvent<Map<String, Object>> event) {
        Object brut = event.payload() == null ? null : event.payload().get("missionId");
        if (brut == null) {
            log.warn("mission.terminee sans missionId écarté (eventId {})", event.eventId());
            return null;
        }
        try {
            return UUID.fromString(brut.toString());
        } catch (IllegalArgumentException e) {
            log.warn("mission.terminee avec un missionId invalide « {} » écarté (eventId {})", brut, event.eventId());
            return null;
        }
    }
}
