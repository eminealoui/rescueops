package tn.esprit.rescueops.common.event;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RescueOpsEventTest {

    record TransfertConfirme(String transfertId, String hopitalId) {
    }

    @Test
    void ofRemplitLEnveloppe() {
        MDC.put("correlationId", "corr-12345678");
        try {
            Instant before = Instant.now();

            RescueOpsEvent<TransfertConfirme> e = RescueOpsEvent.of("transfert.confirme", new TransfertConfirme("t1", "h1"));

            assertThat(e.eventId()).isNotNull();
            assertThat(e.eventType()).isEqualTo("transfert.confirme");
            assertThat(e.version()).isEqualTo(1);
            assertThat(e.occurredAt()).isBetween(before, Instant.now());
            assertThat(e.correlationId()).isEqualTo("corr-12345678");
            assertThat(e.payload().transfertId()).isEqualTo("t1");
        } finally {
            MDC.clear();
        }
    }

    @Test
    void allerRetourJsonConserveLEvenement() {
        JsonMapper json = JsonMapper.builder().findAndAddModules().build();
        RescueOpsEvent<TransfertConfirme> e = RescueOpsEvent.of("transfert.confirme", new TransfertConfirme("t1", "h1"));

        String s = json.writeValueAsString(e);
        RescueOpsEvent<TransfertConfirme> back = json.readValue(s, new TypeReference<RescueOpsEvent<TransfertConfirme>>() {
        });

        assertThat(back).isEqualTo(e);
    }

    @Test
    void autoConfigurationDeclareLesExchanges() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RescueOpsAmqpAutoConfiguration.class))
                .withBean(ConnectionFactory.class, () -> new CachingConnectionFactory("localhost"))
                .run(ctx -> {
                    assertThat(ctx.getBean("rescueOpsEventsExchange", TopicExchange.class).getName())
                            .isEqualTo("rescueops.events");
                    assertThat(ctx.getBean("rescueOpsEventsDlx", DirectExchange.class).getName())
                            .isEqualTo("rescueops.events.dlx");
                    assertThat(ctx.getBean(RabbitTemplate.class).getExchange()).isEqualTo("rescueops.events");
                });
    }

    @Test
    void lEnveloppeNExposePasDeChampsInattendus() {
        JsonMapper json = JsonMapper.builder().build();
        Map<String, Object> fields = json.convertValue(
                RescueOpsEvent.of("capacite.maj", Map.of("serviceId", "s1")), new TypeReference<>() {
                });

        assertThat(fields).containsOnlyKeys("eventId", "eventType", "version", "occurredAt", "correlationId", "payload");
    }
}
