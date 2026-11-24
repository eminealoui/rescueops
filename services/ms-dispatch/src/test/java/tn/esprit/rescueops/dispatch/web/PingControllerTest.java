package tn.esprit.rescueops.dispatch.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rescueops.dispatch.WebSliceTest;
import tn.esprit.rescueops.dispatch.client.ResourceClient;
import tn.esprit.rescueops.dispatch.messaging.IncidentPrioriseListener;
import tn.esprit.rescueops.dispatch.service.AffectationService;
import tn.esprit.rescueops.dispatch.service.ScoringService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Vérifie que le squelette démarre avec la sécurité RescueOps.
 * Base, Flyway, Eureka et RabbitMQ sont désactivés ici : ce test tourne sans Docker.
 */
@WebSliceTest
class PingControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AffectationService affectationService;

    @MockitoBean
    ScoringService scoringService;

    @MockitoBean
    IncidentPrioriseListener incidentPrioriseListener;

    @MockitoBean
    ResourceClient resourceClient;

    @Test
    void sansTokenLaRequeteEstRefusee() throws Exception {
        mvc.perform(get("/api/dispatch/ping"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    }

    @Test
    void avecTokenLeServiceRepondEtReconnaitLUtilisateur() throws Exception {
        mvc.perform(get("/api/dispatch/ping")
                        .with(jwt().jwt(j -> j.subject("utilisateur-test"))
                                .authorities(new SimpleGrantedAuthority("ROLE_OPERATEUR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("ms-dispatch"))
                .andExpect(jsonPath("$.utilisateur").value("utilisateur-test"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_OPERATEUR"));
    }
}
