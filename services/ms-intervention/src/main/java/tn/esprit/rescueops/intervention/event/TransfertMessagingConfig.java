package tn.esprit.rescueops.intervention.event;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tn.esprit.rescueops.common.event.RescueOpsAmqpAutoConfiguration;

@Configuration
public class TransfertMessagingConfig {

    public static final String FILE_TRANSFERT =
        "ms-intervention.transfert-confirme";

    public static final String FILE_ERREUR =
        "ms-intervention.transfert-confirme.dlq";

    public static final String ROUTING_KEY =
        "transfert.confirme";

    @Bean
    public Declarables transfertDeclarables(
        @Qualifier("rescueOpsEventsExchange")
        TopicExchange exchange,

        @Qualifier("rescueOpsEventsDlx")
        DirectExchange dlx
    ) {

        Queue queue = QueueBuilder
            .durable(FILE_TRANSFERT)
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
                .with(ROUTING_KEY),

            BindingBuilder.bind(dlq)
                .to(dlx)
                .with(FILE_ERREUR)
        );
    }
}
