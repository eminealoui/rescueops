
package tn.esprit.rescueops.intervention.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.intervention.domain.EvenementAffectationRecu;
import tn.esprit.rescueops.intervention.repository.EvenementAffectationRecuRepository;
import tn.esprit.rescueops.intervention.service.MissionAutomatiqueService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class AffectationConfirmeeListener {

    private static final Logger log =
        LoggerFactory.getLogger(AffectationConfirmeeListener.class);

    private final EvenementAffectationRecuRepository repository;
    private final MissionAutomatiqueService missionService;

    public AffectationConfirmeeListener(
        EvenementAffectationRecuRepository repository,
        MissionAutomatiqueService missionService
    ) {
        this.repository = repository;
        this.missionService = missionService;
    }

    @RabbitListener(queues = MessagingConfig.FILE_AFFECTATION)
    @Transactional
    public void recevoir(RescueOpsEvent<Map<String, Object>> event) {

        // 1. Vérifier l'événement
        if (event == null || event.eventId() == null
            || event.payload() == null
            || !"affectation.confirmee".equals(event.eventType())) {

            log.warn("Événement affectation invalide");
            return;
        }

        Map<String, Object> payload = event.payload();

        // 2. Lire l'identifiant de l'affectation
        UUID affectationId = lireUuid(payload.get("affectationId"));

        if (affectationId == null) {
            log.warn("affectationId manquant ou invalide");
            return;
        }

        // 3. Rechercher l'événement déjà reçu
        EvenementAffectationRecu recu = repository
            .findById(event.eventId())
            .orElseGet(EvenementAffectationRecu::new);

        if (recu.isTraite()) {
            log.info("Événement déjà traité : {}", event.eventId());
            return;
        }

        // 4. Enregistrer le contenu reçu
        Map<String, Object> contenu = new LinkedHashMap<>();

        contenu.put("eventId", event.eventId().toString());
        contenu.put("eventType", event.eventType());
        contenu.put("version", event.version());
        contenu.put("payload", payload);

        recu.setEventId(event.eventId());
        recu.setAffectationId(affectationId);
        recu.setPayloadJson(contenu);
        recu.setRecuLe(Instant.now());
        recu.setTraite(false);

        repository.save(recu);

        // 5. Lire les autres informations
        UUID incidentId = lireUuid(payload.get("incidentId"));
        UUID ambulanceId = lireUuid(payload.get("ambulanceId"));

        Object valeurAmbulancier =
            payload.get("ambulancierKeycloakId");

        String ambulancierId = valeurAmbulancier == null
            ? null
            : valeurAmbulancier.toString();

        // 6. Vérifier que les informations sont complètes
        if (incidentId == null || ambulanceId == null
            || lireUuid(ambulancierId) == null) {

            log.warn(
                "Affectation {} incomplète : événement conservé en attente",
                affectationId
            );
            return;
        }

        // 7. Créer automatiquement la mission
        missionService.creerDepuisAffectation(
            affectationId,
            incidentId,
            ambulanceId,
            ambulancierId
        );

        // 8. Marquer l'événement comme traité
        recu.setTraite(true);
        repository.save(recu);

        log.info(
            "Mission créée ou retrouvée pour l'affectation {}",
            affectationId
        );
    }

    private UUID lireUuid(Object valeur) {
        if (valeur == null) {
            return null;
        }

        try {
            return UUID.fromString(valeur.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
