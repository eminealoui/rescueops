package tn.esprit.rescueops.dispatch;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tests d'API sans base ni broker : la sécurité RescueOps est réelle, les services métier sont simulés.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration,"
                + "org.springframework.boot.rabbitmq.autoconfigure.RabbitAutoConfiguration",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.health.rabbit.enabled=false",
        "spring.task.scheduling.enabled=false"})
@AutoConfigureMockMvc
public @interface WebSliceTest {
}
