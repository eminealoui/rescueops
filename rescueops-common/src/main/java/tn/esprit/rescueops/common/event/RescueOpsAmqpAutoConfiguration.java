package tn.esprit.rescueops.common.event;

import org.slf4j.MDC;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import tn.esprit.rescueops.common.error.ProblemResponses;
import tn.esprit.rescueops.common.web.CorrelationIdFilter;

/**
 * Messagerie commune : exchange topic {@code rescueops.events}, dead-letter exchange, JSON,
 * et propagation de l'identifiant de corrélation dans l'en-tête des messages.
 */
@AutoConfiguration(beforeName = "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
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

    @Bean
    @ConditionalOnMissingBean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setExchange(EVENTS_EXCHANGE);
        template.setMessageConverter(converter);
        template.setBeforePublishPostProcessors(message -> {
            String id = MDC.get(ProblemResponses.MDC_CORRELATION_ID);
            if (id != null) {
                message.getMessageProperties().setHeader(CorrelationIdFilter.HEADER, id);
            }
            return message;
        });
        return template;
    }
}
