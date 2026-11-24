package tn.esprit.rescueops.dispatch.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tn.esprit.rescueops.common.event.RescueOpsAmqpAutoConfiguration;

/**
 * Configuration des files RabbitMQ pour ms-dispatch.
 * <p>
 * Déclare la file durable {@code ms-dispatch.incident.priorise} et la lie
 * à l'exchange commun {@code rescueops.events} avec la routing key {@code incident.priorise}.
 * En cas d'échec répété, les messages sont envoyés vers la dead-letter queue (.dlq).
 */
@Configuration
public class MessagingConfig {

    public static final String QUEUE_INCIDENT_PRIORISE = "ms-dispatch.incident.priorise";
    public static final String QUEUE_DLQ = QUEUE_INCIDENT_PRIORISE + ".dlq";

    @Bean
    Queue queueIncidentPriorise() {
        return QueueBuilder.durable(QUEUE_INCIDENT_PRIORISE)
                .deadLetterExchange(RescueOpsAmqpAutoConfiguration.DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(QUEUE_DLQ)
                .build();
    }

    @Bean
    Queue queueDlqIncidentPriorise() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    Binding bindingIncidentPriorise(Queue queueIncidentPriorise, TopicExchange rescueOpsEventsExchange) {
        return BindingBuilder.bind(queueIncidentPriorise)
                .to(rescueOpsEventsExchange)
                .with("incident.priorise");
    }

    @Bean
    Binding bindingDlqIncidentPriorise(Queue queueDlqIncidentPriorise, DirectExchange rescueOpsEventsDlx) {
        return BindingBuilder.bind(queueDlqIncidentPriorise)
                .to(rescueOpsEventsDlx)
                .with(QUEUE_DLQ);
    }
}
