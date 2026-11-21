
package tn.esprit.rescueops.intervention.event;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagingConfig {

    public static final String FILE_AFFECTATION =
        "ms-intervention.affectation-confirmee";

    @Bean
    public Queue fileAffectationConfirmee() {
        return QueueBuilder
            .durable(FILE_AFFECTATION)
            .build();
    }

    @Bean
    public Binding liaisonAffectationConfirmee(
        Queue fileAffectationConfirmee,
        TopicExchange rescueOpsEventsExchange
    ) {
        return BindingBuilder
            .bind(fileAffectationConfirmee)
            .to(rescueOpsEventsExchange)
            .with("affectation.confirmee");
    }
}
