package tn.esprit.rescueops.common.event;

import org.slf4j.MDC;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.rabbitmq.autoconfigure.RabbitTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import tn.esprit.rescueops.common.error.ProblemResponses;
import tn.esprit.rescueops.common.web.CorrelationIdFilter;

/**
 * Messagerie commune : exchange topic {@code rescueops.events}, dead-letter exchange, JSON,
 * et propagation de l'identifiant de corrélation dans l'en-tête des messages.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration")
@ConditionalOnClass(RabbitTemplate.class)
public class RescueOpsAmqpAutoConfiguration {

    public static final String EVENTS_EXCHANGE = "rescueops.events";
    public static final String DEAD_LETTER_EXCHANGE = "rescueops.events.dlx";

    @Bean
    TopicExchange rescueOpsEventsExchange() {
        return ExchangeBuilder.topicExchange(EVENTS_EXCHANGE).durable(true).build();
    }

    @Bean
    DirectExchange rescueOpsEventsDlx() {
        return ExchangeBuilder.directExchange(DEAD_LETTER_EXCHANGE).durable(true).build();
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    MessageConverter rescueOpsMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Personnalise le RabbitTemplate de Spring Boot au lieu de le remplacer :
     * les propriétés {@code spring.rabbitmq.template.*} (retry, mandatory…) restent actives.
     * L'exchange par défaut n'est imposé que si le service n'en a pas configuré.
     */
    @Bean
    RabbitTemplateCustomizer rescueOpsRabbitTemplateCustomizer() {
        return template -> {
            if (template.getExchange() == null || template.getExchange().isEmpty()) {
                template.setExchange(EVENTS_EXCHANGE);
            }
            template.addBeforePublishPostProcessors(message -> {
                String id = MDC.get(ProblemResponses.MDC_CORRELATION_ID);
                if (id != null) {
                    message.getMessageProperties().setHeader(CorrelationIdFilter.HEADER, id);
                }
                return message;
            });
        };
    }
}
