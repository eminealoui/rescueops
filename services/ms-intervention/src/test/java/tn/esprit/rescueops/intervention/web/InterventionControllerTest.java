package tn.esprit.rescueops.intervention.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tn.esprit.rescueops.intervention.domain.Intervention;
import tn.esprit.rescueops.intervention.repository.InterventionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de la couche web du CRUD Intervention (Module 4).
 * <p>
 * Le jeu de départ statique est fourni par la migration Flyway V2 ; ici le repository est simulé
 * pour vérifier le contrat HTTP sans dépendre de MySQL.
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
@DisplayName("InterventionController – Tests de la couche web")
class InterventionControllerTest {

    private static final String ID_STATIQUE = "11111111-1111-4111-8111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterventionRepository interventionRepository;

    private static RequestPostProcessor as(String role) {
        return jwt().jwt(j -> j.subject("u-" + role)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    private static Intervention interventionStatique() {
        Intervention intervention = new Intervention();
        intervention.setId(ID_STATIQUE);
        intervention.setIncidentId(ID_STATIQUE);
        intervention.setTitre("Intervention statique - test dispatch CRUD");
        intervention.setTypeIntervention("URGENCE");
        intervention.setLatitude(36.8065);
        intervention.setLongitude(10.1815);
        intervention.setStatut("EN_ATTENTE");
        intervention.setCreatedAt(Instant.parse("2026-11-26T15:00:00Z"));
        return intervention;
    }

    @Test
    @DisplayName("GET /api/interventions sans token → 401")
    void lister_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/interventions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/interventions avec rôle CITOYEN → 403")
    void lister_mauvaisRole_403() throws Exception {
        mockMvc.perform(get("/api/interventions").with(as("CITOYEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/interventions avec OPERATEUR → 200 + intervention statique")
    void lister_operateur_200() throws Exception {
        Mockito.when(interventionRepository.findAll()).thenReturn(List.of(interventionStatique()));

        mockMvc.perform(get("/api/interventions").with(as("OPERATEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ID_STATIQUE))
                .andExpect(jsonPath("$[0].titre").value("Intervention statique - test dispatch CRUD"))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));
    }

    @Test
    @DisplayName("GET /api/interventions/{id} introuvable → 404")
    void get_introuvable_404() throws Exception {
        Mockito.when(interventionRepository.findById("INCONNU")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/interventions/INCONNU").with(as("OPERATEUR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVENTION_INTROUVABLE"));
    }

    @Test
    @DisplayName("POST /api/interventions avec OPERATEUR → 201 + statut EN_ATTENTE")
    void creer_operateur_201() throws Exception {
        Mockito.when(interventionRepository.save(any(Intervention.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String body = """
                {
                  "incidentId": "INC-CRUD-001",
                  "titre": "Intervention créée par test",
                  "typeIntervention": "SECOURS",
                  "latitude": 36.8,
                  "longitude": 10.18
                }
                """;

        mockMvc.perform(post("/api/interventions")
                        .with(as("OPERATEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value("INC-CRUD-001"))
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    @DisplayName("PATCH /api/interventions/{id} avec OPERATEUR → 200 + titre/statut modifiés")
    void modifier_operateur_200() throws Exception {
        Mockito.when(interventionRepository.findById(ID_STATIQUE))
                .thenReturn(Optional.of(interventionStatique()));
        Mockito.when(interventionRepository.save(any(Intervention.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(patch("/api/interventions/" + ID_STATIQUE)
                        .with(as("OPERATEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Titre modifié\",\"statut\":\"EN_COURS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titre").value("Titre modifié"))
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
    }

    @Test
    @DisplayName("DELETE /api/interventions/{id} avec ADMIN → 204")
    void supprimer_admin_204() throws Exception {
        Mockito.when(interventionRepository.findById(ID_STATIQUE))
                .thenReturn(Optional.of(interventionStatique()));

        mockMvc.perform(delete("/api/interventions/" + ID_STATIQUE).with(as("ADMIN")))
                .andExpect(status().isNoContent());

        Mockito.verify(interventionRepository).delete(Mockito.any(Intervention.class));
    }
}
