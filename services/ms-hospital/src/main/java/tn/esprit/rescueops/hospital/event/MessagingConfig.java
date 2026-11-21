package tn.esprit.rescueops.hospital.event;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.rescueops.common.event.RescueOpsAmqpAutoConfiguration;

/**
 * Files RabbitMQ du module. Chaque service possède SES files, liées à l'exchange commun rescueops.events.
 * Un message qui échoue 3 fois part dans la file d'échecs (.dlq) au lieu de boucler : on peut l'inspecter
 * dans la console RabbitMQ.
 */
@Configuration
public class MessagingConfig {

    public static final String FILE_MISSION_TERMINEE = "ms-hospital.mission-terminee";
    public static final String FILE_ECHECS = FILE_MISSION_TERMINEE + ".dlq";

    @Bean
    Queue fileMissionTerminee() {
        return QueueBuilder.durable(FILE_MISSION_TERMINEE)
                .deadLetterExchange(RescueOpsAmqpAutoConfiguration.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(FILE_ECHECS)
                .build();
    }

    @Bean
    Queue fileEchecsMissionTerminee() {
        return QueueBuilder.durable(FILE_ECHECS).build();
    }

    @Bean
    Binding liaisonMissionTerminee(Queue fileMissionTerminee, TopicExchange rescueOpsEventsExchange) {
        return BindingBuilder.bind(fileMissionTerminee).to(rescueOpsEventsExchange).with("mission.terminee");
    }

    @Bean
    Binding liaisonEchecs(Queue fileEchecsMissionTerminee, DirectExchange rescueOpsEventsDlx) {
        return BindingBuilder.bind(fileEchecsMissionTerminee).to(rescueOpsEventsDlx).with(FILE_ECHECS);
    }
}
