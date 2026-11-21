
package tn.esprit.rescueops.intervention.event;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.common.event.RescueOpsAmqpAutoConfiguration;

import java.util.Map;
import java.util.UUID;

@Component
public class MissionEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public MissionEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publierChangementStatut(
        UUID missionId,
        String ancienStatut,
        String nouveauStatut
    ) {
        Map<String, String> payload = Map.of(
            "missionId", missionId.toString(),
            "ancienStatut", ancienStatut,
            "nouveauStatut", nouveauStatut
        );

        RescueOpsEvent<Map<String, String>> event =
            RescueOpsEvent.of("mission.statut", payload);

        rabbitTemplate.convertAndSend(
            RescueOpsAmqpAutoConfiguration.EVENTS_EXCHANGE,
            "mission.statut",
            event
        );
    }

    public void publierMissionTerminee(UUID missionId) {

        Map<String, String> payload = Map.of(
            "missionId", missionId.toString()
        );

        RescueOpsEvent<Map<String, String>> event =
            RescueOpsEvent.of(
                "mission.terminee",
                payload
            );

        rabbitTemplate.convertAndSend(
            RescueOpsAmqpAutoConfiguration.EVENTS_EXCHANGE,
            "mission.terminee",
            event
        );
    }

}
