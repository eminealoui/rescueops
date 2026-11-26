package tn.esprit.rescueops.intervention.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rescueops.intervention.repository.InterventionRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Vérifie que le squelette démarre avec la sécurité RescueOps.
 * Base, Flyway, Eureka et RabbitMQ sont désactivés ici : ce test tourne sans Docker.
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.health.rabbit.enabled=false"})
@AutoConfigureMockMvc
class PingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    InterventionRepository interventionRepository;

    @Test
    void sansTokenLaRequeteEstRefusee() throws Exception {
        mvc.perform(get("/api/interventions/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    }

    @Test
    void avecTokenLeServiceRepondEtReconnaitLUtilisateur() throws Exception {
        mvc.perform(get("/api/interventions/ping")
                        .with(jwt().jwt(j -> j.subject("utilisateur-test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_AMBULANCIER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ms-intervention"))
                .andExpect(jsonPath("$.utilisateur").value("utilisateur-test"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_AMBULANCIER"));
    }
}
