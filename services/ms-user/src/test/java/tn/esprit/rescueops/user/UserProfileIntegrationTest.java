package tn.esprit.rescueops.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tn.esprit.rescueops.user.repository.UserProfileRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Intégration avec un VRAI MySQL 8.4 (même version que le socle) : migration Flyway, validation Hibernate,
 * contraintes d'unicité et concurrence. Nécessite Docker.
 */
@Testcontainers
@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
@AutoConfigureMockMvc
class UserProfileIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4.13");

    @Autowired
    MockMvc mvc;

    @Autowired
    UserProfileRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    private static RequestPostProcessor user(String sub, String email, String given, String family) {
        return jwt().jwt(j -> j.subject(sub).claim("email", email).claim("given_name", given).claim("family_name", family))
                .authorities(new SimpleGrantedAuthority("ROLE_CITOYEN"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("sub-admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void flywayEtValidationHibernateDemarrent() {
        // Le simple démarrage du contexte prouve que V1__creation_user_profile.sql correspond à l'entité (ddl-auto=validate).
        assertThat(repository.count()).isZero();
    }

    @Test
    void deuxPremiersAccesSimultanesCreentUnSeulProfil() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return mvc.perform(get("/api/users/me").with(user("sub-race", "race@rescueops.test", "R", "C")))
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        for (Future<Integer> f : results) {
            assertThat(f.get(30, TimeUnit.SECONDS)).isEqualTo(200);
        }
        pool.shutdown();

        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void rechercheParNomInsensibleALaCasse() throws Exception {
        mvc.perform(get("/api/users/me").with(user("s1", "meriem@rescueops.test", "Meriem", "Ben Salah")));
        mvc.perform(get("/api/users/me").with(user("s2", "sami@rescueops.test", "Sami", "Trabelsi")));
        mvc.perform(get("/api/users/me").with(user("s3", "leila@rescueops.test", "Leila", "Bouzid")));

        mvc.perform(get("/api/users").param("query", "ben").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].lastName").value("Ben Salah"));
    }

    @Test
    void emailDejaPrisParUnAutreProfil409() throws Exception {
        mvc.perform(get("/api/users/me").with(user("sub-A", "x@rescueops.test", "A", "A"))).andExpect(status().isOk());

        mvc.perform(get("/api/users/me").with(user("sub-B", "x@rescueops.test", "B", "B")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLIT_DONNEES"));
    }
}
