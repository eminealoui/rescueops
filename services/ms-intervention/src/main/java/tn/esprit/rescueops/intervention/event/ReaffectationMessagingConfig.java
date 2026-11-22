
package tn.esprit.rescueops.intervention.event;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tn.esprit.rescueops.common.event.RescueOpsAmqpAutoConfiguration;

@Configuration
public class ReaffectationMessagingConfig {

    public static final String FILE_DECISION =
        "ms-intervention.reaffectation-decision";

    public static final String FILE_ERREUR =
        "ms-intervention.reaffectation-decision.dlq";

    public static final String CONFIRMEE =
        "mission.reaffectation.confirmee";

    public static final String REFUSEE =
        "mission.reaffectation.refusee";

    @Bean
    public Declarables reaffectationDeclarables(
        @Qualifier("rescueOpsEventsExchange")
        TopicExchange exchange,

        @Qualifier("rescueOpsEventsDlx")
        DirectExchange dlx
    ) {

        Queue queue = QueueBuilder
            .durable(FILE_DECISION)
            .deadLetterExchange(
                RescueOpsAmqpAutoConfiguration.DEAD_LETTER_EXCHANGE
            )
            .deadLetterRoutingKey(FILE_ERREUR)
            .build();

        Queue dlq = QueueBuilder
            .durable(FILE_ERREUR)
            .build();

        return new Declarables(
            queue,
            dlq,

            BindingBuilder.bind(queue)
                .to(exchange)
                .with(CONFIRMEE),

            BindingBuilder.bind(queue)
                .to(exchange)
                .with(REFUSEE),

            BindingBuilder.bind(dlq)
                .to(dlx)
                .with(FILE_ERREUR)
        );
    }
}
