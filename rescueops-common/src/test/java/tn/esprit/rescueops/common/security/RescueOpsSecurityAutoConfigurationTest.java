package tn.esprit.rescueops.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = RescueOpsSecurityAutoConfigurationTest.TestApp.class)
@AutoConfigureMockMvc
class RescueOpsSecurityAutoConfigurationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestController.class)
    static class TestApp {
    }

    @RestController
    static class TestController {
        @GetMapping("/secure")
        String secure() {
            return "ok";
        }

        @GetMapping("/admin")
        @PreAuthorize("hasRole('ADMIN')")
        String admin() {
            return "admin";
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void sansTokenRenvoie401AuFormatProblemDetail() throws Exception {
        mvc.perform(get("/secure"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    }

    @Test
    void avecTokenRenvoie200() throws Exception {
        mvc.perform(get("/secure").with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void roleInsuffisantRenvoie403AuFormatProblemDetail() throws Exception {
        mvc.perform(get("/admin").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CITOYEN"))))
                .andExpect(status().isForbidden())
                .andExpect(header().string("Content-Type", startsWith("application/problem+json")))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ACCES_REFUSE"));
    }

    @Test
    void roleAdminAutorise() throws Exception {
        mvc.perform(get("/admin").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    void healthEstPublic() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
