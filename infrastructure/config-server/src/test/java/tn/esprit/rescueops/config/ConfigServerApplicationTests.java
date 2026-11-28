package tn.esprit.rescueops.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.config.server.native.search-locations=file:../../config-repo/"
        })
class ConfigServerApplicationTests {

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
    void sertLaConfigCommuneEtCelleDuService() {
        var response = rest.build().get().uri("http://localhost:" + port + "/ms-hospital/default")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("ms-hospital.yml")
                .contains("application.yml")
                .contains("config-server");
    }
}
