package tn.esprit.rescueops.emergency.event;

import java.util.Map;
import java.util.UUID;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.emergency.service.AffectationEventService;

@Component
public class AffectationConfirmeeListener {

    private final AffectationEventService service;

    public AffectationConfirmeeListener(
            AffectationEventService service
    ) {

        this.service = service;
    }

    @RabbitListener(
            queues = MessagingConfig.QUEUE
    )
    public void recevoir(
            RescueOpsEvent<Map<String, Object>> event
    ) {

        if (event == null
                || event.eventId() == null
                || event.version() != 1
                || !MessagingConfig.AFFECTATION_CONFIRMEE
                        .equals(event.eventType())
                || event.payload() == null) {

            throw new AmqpRejectAndDontRequeueException(
                    "Evenement invalide"
            );
        }

        Object valeur =
                event.payload().get("incidentId");

        if (valeur == null) {

            throw new AmqpRejectAndDontRequeueException(
                    "incidentId manquant"
            );
        }

        try {

            UUID incidentId =
                    UUID.fromString(valeur.toString());

            service.traiter(
                    event.eventId(),
                    incidentId
            );

        } catch (
                IllegalArgumentException
                | NotFoundException
                | BusinessRuleException ex
        ) {

            throw new AmqpRejectAndDontRequeueException(
                    "Evenement affectation invalide",
                    ex
            );
        }
    }
}