package tn.esprit.rescueops.dispatch.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.dispatch.service.ScoringService;

/**
 * Consommateur RabbitMQ de l'événement {@code incident.priorise}.
 * <p>
 * Publié par ms-emergency dès qu'un incident est priorisé, ce listener
 * déclenche automatiquement le calcul des recommandations (BF-07) pour
 * que l'opérateur trouve déjà une liste triée quand il ouvre l'écran.
 * <p>
 * Queue : {@code ms-dispatch.incident.priorise}
 * Exchange : {@code rescueops.events} (topic)
 * Routing key : {@code incident.priorise}
 */
@Component
public class IncidentPrioriseListener {

    private static final Logger log = LoggerFactory.getLogger(IncidentPrioriseListener.class);

    private final ScoringService scoringService;

    public IncidentPrioriseListener(ScoringService scoringService) {
        this.scoringService = scoringService;
    }

    /**
     * Traite l'événement incident.priorise reçu via RabbitMQ.
     * Lance le calcul de scoring pour le nouvel incident.
     *
     * @param event enveloppe RescueOpsEvent contenant le payload IncidentPriorisePayload
     */
    @RabbitListener(queues = "ms-dispatch.incident.priorise")
    public void onIncidentPriorise(RescueOpsEvent<IncidentPriorisePayload> event) {
        log.info("[dispatch] Incident priorisé reçu : eventId={} incidentId={}",
                event.eventId(), event.payload().incidentId());
        try {
            scoringService.calculerRecommandations(
                    event.payload().incidentId(),
                    event.payload().latitude(),
                    event.payload().longitude(),
                    null // tous types de ressources
            );
            log.info("[dispatch] Recommandations calculées pour incidentId={}", event.payload().incidentId());
        } catch (Exception e) {
            log.error("[dispatch] Erreur lors du calcul des recommandations pour incidentId={} : {}",
                    event.payload().incidentId(), e.getMessage(), e);
            // On ne relance pas l'exception pour ne pas bloquer la queue RabbitMQ
        }
    }

    /**
     * Payload de l'événement incident.priorise publié par ms-emergency.
     */
    public record IncidentPriorisePayload(
            String incidentId,
            String titre,
            String niveau,       // CRITIQUE, ELEVE, MOYEN, FAIBLE
            Double latitude,
            Double longitude,
            String typeRessourceRequis
    ) {}
}
