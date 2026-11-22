package tn.esprit.rescueops.intervention.event;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.NotFoundException;

import tn.esprit.rescueops.intervention.service.DestinationHospitaliereService;

import java.util.Map;
import java.util.UUID;

@Component
public class TransfertConfirmeListener {

    private final DestinationHospitaliereService service;

    public TransfertConfirmeListener(
        DestinationHospitaliereService service
    ) {
        this.service = service;
    }

    @RabbitListener(
        queues = TransfertMessagingConfig.FILE_TRANSFERT
    )
    public void recevoir(
        RescueOpsEvent<Map<String, Object>> event
    ) {

        try {

            if (event == null
                || event.eventId() == null
                || event.version() != 1
                || event.occurredAt() == null
                || event.payload() == null
                || !"transfert.confirme".equals(event.eventType())) {

                throw new IllegalArgumentException(
                    "Evenement transfert invalide"
                );
            }

            Map<String, Object> payload = event.payload();

            if (!"CONFIRMEE".equals(payload.get("statut"))) {
                throw new IllegalArgumentException(
                    "Le transfert doit etre confirme"
                );
            }

            UUID missionId = lireUUID(payload, "missionId");

            UUID transfertId = lireUUID(payload, "transfertId");

            UUID serviceId = lireUUID(payload, "serviceId");

            service.confirmerDestination(
                missionId,
                transfertId,
                serviceId,
                event.occurredAt()
            );

            System.out.println(
                "[HOSPITAL] Confirmation traitee pour mission : "
                    + missionId
            );

        } catch (IllegalArgumentException |
                 NotFoundException |
                 ConflictException e) {

            throw new AmqpRejectAndDontRequeueException(
                "Evenement hospitalier invalide",
                e
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
