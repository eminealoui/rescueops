package tn.esprit.rescueops.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    RestClient.Builder rest;

    @Test
    void healthEstUp() {
        var response = rest.build().get().uri("http://localhost:" + port + "/actuator/health")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"UP\"");
    }

    @Test
    void registreEurekaRepondEtEstVideAuDemarrage() {
        var response = rest.build().get().uri("http://localhost:" + port + "/eureka/apps")
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"applications\"").doesNotContain("\"instanceId\"");
    }
}
