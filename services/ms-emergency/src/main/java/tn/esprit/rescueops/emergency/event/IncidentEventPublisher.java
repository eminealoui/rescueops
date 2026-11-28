package tn.esprit.rescueops.emergency.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tn.esprit.rescueops.common.event.RescueOpsEvent;

import java.util.Map;

@Component
public class IncidentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public IncidentEventPublisher(
            RabbitTemplate rabbitTemplate
    ) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publier(
            IncidentPrioriseEvent event
    ) {

        rabbitTemplate.convertAndSend(

                "rescueops.events",

                MessagingConfig.INCIDENT_PRIORISE,

                RescueOpsEvent.of(
                        MessagingConfig.INCIDENT_PRIORISE,
                        Map.of(
                                "incidentId",
                                event.incidentId().toString()
                        )
                )
        );
    }
}