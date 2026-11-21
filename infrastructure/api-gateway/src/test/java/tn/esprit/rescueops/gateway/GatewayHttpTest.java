package tn.esprit.rescueops.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

/**
 * Comportement HTTP de la Gateway sans Eureka : aucune instance n'est disponible,
 * ce qui permet de tester sécurité, CORS, corrélation et erreurs 503.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"})
@AutoConfigureWebTestClient
class GatewayHttpTest {

    @Autowired
    WebTestClient client;

    @Test
    void sansTokenRenvoie401() {
        client.get().uri("/api/hospitals/x").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody().jsonPath("$.code").isEqualTo("NON_AUTHENTIFIE");
    }

    @Test
    void serviceSansInstanceRenvoie503ProblemDetail() {
        client.mutateWith(mockJwt()).get().uri("/api/hospitals/x").exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.code").isEqualTo("SERVICE_INDISPONIBLE")
                .jsonPath("$.detail").value(d -> org.assertj.core.api.Assertions.assertThat((String) d).contains("ms-hospital"))
                .jsonPath("$.correlationId").exists();
    }

    @Test
    void routeInconnueRenvoie404ProblemDetail() {
        client.mutateWith(mockJwt()).get().uri("/api/inconnu").exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void correlationIdEstRenvoye() {
        client.get().uri("/api/hospitals/x").header("X-Correlation-Id", "abc12345-gw").exchange()
                .expectHeader().valueEquals("X-Correlation-Id", "abc12345-gw");
    }

    @Test
    void correlationIdDangereuxEstRemplace() {
        client.get().uri("/api/hospitals/x").header("X-Correlation-Id", "<script>").exchange()
                .expectHeader().value("X-Correlation-Id", v ->
                        org.assertj.core.api.Assertions.assertThat(v).doesNotContain("<").hasSize(36));
    }

    @Test
    void healthEstPublic() {
        client.get().uri("/actuator/health").exchange().expectStatus().isOk();
    }

    /** Revue finale n°1 : /docs ne doit pas devenir un proxy public vers tout le service. */
    @Test
    void docsNExposePasLesRoutesMetierSansToken() {
        client.get().uri("/docs/ms-hospital/api/hospitals/1").exchange()
                .expectStatus().isUnauthorized();
        client.get().uri("/docs/ms-user/actuator/env").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void laDocumentationOpenApiResteAccessibleSansToken() {
        // Pas d'instance en test : 503 prouve que la route est publique et ciblée (et non un 401).
        client.get().uri("/docs/ms-hospital/v3/api-docs").exchange()
                .expectStatus().isEqualTo(503);
    }
}
