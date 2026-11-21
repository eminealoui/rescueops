
package tn.esprit.rescueops.intervention.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import tn.esprit.rescueops.common.security.RescueOpsSecurityAutoConfiguration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PingController.class)
@Import(RescueOpsSecurityAutoConfiguration.class)
class PingControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void sansTokenLaRequeteEstRefusee() throws Exception {

        mvc.perform(get("/api/interventions/ping"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code")
                .value("NON_AUTHENTIFIE"));
    }

    @Test
    void avecTokenLeServiceRepondEtReconnaitLUtilisateur()
        throws Exception {

        mvc.perform(get("/api/interventions/ping")
                .with(jwt()
                    .jwt(j -> j.subject("utilisateur-test"))
                    .authorities(
                        new SimpleGrantedAuthority(
                            "ROLE_AMBULANCIER"
                        )
                    )))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.service")
                .value("ms-intervention"))
            .andExpect(jsonPath("$.utilisateur")
                .value("utilisateur-test"))
            .andExpect(jsonPath("$.roles[0]")
                .value("ROLE_AMBULANCIER"));
    }
}
