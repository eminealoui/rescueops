package tn.esprit.rescueops.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Revue finale n°4 : un membre qui déclare SA chaîne de sécurité doit pouvoir garder
 * les rôles Keycloak et les erreurs ProblemDetail en appelant RescueOpsSecurity.applyDefaults.
 */
@SpringBootTest(classes = CustomSecurityChainTest.TestApp.class,
        properties = "management.health.rabbit.enabled=false")
@AutoConfigureMockMvc
class CustomSecurityChainTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(TestController.class)
    static class TestApp {
        /** Décodeur factice : renvoie un JWT Keycloak avec le rôle ADMIN, pour tester la vraie conversion. */
        @Bean
        JwtDecoder fakeDecoder() {
            return token -> Jwt.withTokenValue(token).header("alg", "RS256").subject("u-1")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60))
                    .claim("realm_access", Map.of("roles", List.of("ADMIN"))).build();
        }

        @Bean
        SecurityFilterChain memberChain(HttpSecurity http, RescueOpsSecurityProperties props) throws Exception {
            http.authorizeHttpRequests(a -> a.requestMatchers("/public/**").permitAll());
            return RescueOpsSecurity.applyDefaults(http, props).build();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/public/info")
        String info() {
            return "info";
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
    void lesRolesKeycloakRestentConvertis() throws Exception {
        mvc.perform(get("/admin").header("Authorization", "Bearer faux-token"))
                .andExpect(status().isOk());
    }

    @Test
    void laRegleAjouteeParLeMembreEstAppliquee() throws Exception {
        mvc.perform(get("/public/info")).andExpect(status().isOk());
    }

    @Test
    void le401ResteAuFormatProblemDetail() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("NON_AUTHENTIFIE"));
    }
}
