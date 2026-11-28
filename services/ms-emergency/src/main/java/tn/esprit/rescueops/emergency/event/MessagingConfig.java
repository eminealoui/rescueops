package tn.esprit.rescueops.emergency.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    public static final String QUEUE =
            "ms-emergency.evenements";

    public static final String AFFECTATION_CONFIRMEE =
            "affectation.confirmee";

    public static final String INCIDENT_PRIORISE =
            "incident.priorise";

    @Bean
    public Queue emergencyQueue() {

        return QueueBuilder
                .durable(QUEUE)
                .build();
    }

    @Bean
    public Binding emergencyBinding(

            @Qualifier("emergencyQueue")
            Queue emergencyQueue,

            @Qualifier("rescueOpsEventsExchange")
            TopicExchange exchange
    ) {

        return BindingBuilder
                .bind(emergencyQueue)
                .to(exchange)
                .with(AFFECTATION_CONFIRMEE);
    }
}