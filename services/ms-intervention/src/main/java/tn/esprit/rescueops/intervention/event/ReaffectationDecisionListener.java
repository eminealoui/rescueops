
package tn.esprit.rescueops.intervention.event;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.NotFoundException;

import tn.esprit.rescueops.intervention.service.DemandeReaffectationService;

import java.util.Map;
import java.util.UUID;

@Component
public class ReaffectationDecisionListener {

    private final DemandeReaffectationService service;

    public ReaffectationDecisionListener(
        DemandeReaffectationService service
    ) {
        this.service = service;
    }

    @RabbitListener(
        queues = ReaffectationMessagingConfig.FILE_DECISION
    )
    public void recevoir(
        RescueOpsEvent<Map<String, Object>> event
    ) {

        try {

            if (event == null || event.eventId() == null
                || event.version() != 1
                || event.payload() == null) {

                throw new IllegalArgumentException(
                    "Evenement invalide"
                );
            }

            Map<String, Object> payload = event.payload();

            UUID demandeId = lireUUID(
                payload, "demandeId"
            );

            UUID missionId = lireUUID(
                payload, "missionId"
            );

            UUID ancienneAffectationId = lireUUID(
                payload, "ancienneAffectationId"
            );

            if (ReaffectationMessagingConfig.CONFIRMEE
                .equals(event.eventType())) {

                service.traiterDecision(
                    demandeId,
                    missionId,
                    ancienneAffectationId,
                    true,
                    lireUUID(payload, "nouvelleAffectationId"),
                    lireUUID(payload, "nouvelleAmbulanceId"),
                    lireUUID(
                        payload,
                        "nouvelAmbulancierKeycloakId"
                    ).toString()
                );

            } else if (ReaffectationMessagingConfig.REFUSEE
                .equals(event.eventType())) {

                service.traiterDecision(
                    demandeId,
                    missionId,
                    ancienneAffectationId,
                    false,
                    null,
                    null,
                    null
                );

            } else {

                throw new IllegalArgumentException(
                    "Type evenement non supporte"
                );
            }

            System.out.println(
                "[REAFFECTATION] Decision traitee : "
                    + event.eventType()
                    + " - " + demandeId
            );

        } catch (IllegalArgumentException |
                 ConflictException |
                 NotFoundException e) {

            throw new AmqpRejectAndDontRequeueException(
                "Decision de reaffectation invalide", e
            );
        }
    }

    private UUID lireUUID(
        Map<String, Object> payload,
        String champ
    ) {

        Object valeur = payload.get(champ);

        if (valeur == null) {

            throw new IllegalArgumentException(
                "Champ obligatoire manquant : " + champ
            );
        }

        return UUID.fromString(valeur.toString());
    }
}
