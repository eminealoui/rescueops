package tn.esprit.rescueops.dispatch.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.dispatch.WebSliceTest;
import tn.esprit.rescueops.dispatch.client.ResourceClient;
import tn.esprit.rescueops.dispatch.domain.StatutAffectation;
import tn.esprit.rescueops.dispatch.dto.AffectationRequest;
import tn.esprit.rescueops.dispatch.dto.AffectationResponse;
import tn.esprit.rescueops.dispatch.dto.RecommandationResponse;
import tn.esprit.rescueops.dispatch.messaging.IncidentPrioriseListener;
import tn.esprit.rescueops.dispatch.service.AffectationService;
import tn.esprit.rescueops.dispatch.service.ScoringService;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de la couche web du module Dispatching Intelligent (BF-07, BF-08, BF-09).
 * <p>
 * Stratégie : @WebSliceTest — contrôleur avec sécurité réelle (JWT), services mockés avec @MockitoBean.
 * Scénarios testés : 401 (non authentifié), 403 (mauvais rôle), 200/201 (nominaux),
 * 404 (ressource introuvable), 409 (conflit).
 */
@WebSliceTest
@DisplayName("AffectationController – Tests de la couche web")
class AffectationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AffectationService affectationService;

    @MockitoBean
    private ScoringService scoringService;

    @MockitoBean
    private IncidentPrioriseListener incidentPrioriseListener;

    @MockitoBean
    private ResourceClient resourceClient;

    private static RequestPostProcessor as(String role) {
        return jwt().jwt(j -> j.subject("u-" + role)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 401 — Non authentifié
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /recommandations/{incidentId} sans token → 401")
    void recommandations_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/dispatch/recommandations/INC-001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /affectations sans token → 401")
    void affectations_sansToken_401() throws Exception {
        mockMvc.perform(get("/api/dispatch/affectations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /affectations sans token → 401")
    void creerAffectation_sansToken_401() throws Exception {
        AffectationRequest request = new AffectationRequest("INC-001", "RES-001", "AMBULANCE");
        mockMvc.perform(post("/api/dispatch/affectations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 403 — Mauvais rôle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /recommandations/{incidentId} avec rôle CITOYEN → 403")
    void recommandations_mauvaisRole_403() throws Exception {
        mockMvc.perform(get("/api/dispatch/recommandations/INC-001").with(as("CITOYEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /affectations avec rôle AMBULANCIER → 403")
    void creerAffectation_mauvaisRole_403() throws Exception {
        AffectationRequest request = new AffectationRequest("INC-001", "RES-001", "AMBULANCE");
        mockMvc.perform(post("/api/dispatch/affectations")
                        .with(as("AMBULANCIER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 200 / 201 — Cas nominaux
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /recommandations/{incidentId} avec OPERATEUR → 200 + liste")
    void recommandations_operateur_200() throws Exception {
        List<RecommandationResponse> recs = List.of(
                new RecommandationResponse(1L, "INC-001", "RES-001", "Ambulance A1",
                        "AMBULANCE", 0.87, "Score 0.87 | Distance 2.1km | Dispo: ✓ | Compétences: ✓",
                        2.1, true, true, Instant.now())
        );
        Mockito.when(affectationService.getRecommandations("INC-001")).thenReturn(recs);

        mockMvc.perform(get("/api/dispatch/recommandations/INC-001").with(as("OPERATEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ressourceId").value("RES-001"))
                .andExpect(jsonPath("$[0].score").value(0.87));
    }

    @Test
    @DisplayName("POST /affectations avec OPERATEUR → 201 + affectation PROPOSEE")
    void creerAffectation_operateur_201() throws Exception {
        AffectationRequest request = new AffectationRequest("INC-001", "RES-001", "AMBULANCE");
        AffectationResponse response = new AffectationResponse(
                1L, "INC-001", "RES-001", "AMBULANCE",
                StatutAffectation.PROPOSEE, null, null,
                "u-OPERATEUR", Instant.now(), Instant.now()
        );
        Mockito.when(affectationService.creerAffectation(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/dispatch/affectations")
                        .with(as("OPERATEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("PROPOSEE"))
                .andExpect(jsonPath("$.incidentId").value("INC-001"));
    }

    @Test
    @DisplayName("PATCH /affectations/{id}/confirmer avec OPERATEUR → 200 + statut CONFIRMEE")
    void confirmerAffectation_operateur_200() throws Exception {
        AffectationResponse response = new AffectationResponse(
                1L, "INC-001", "RES-001", "AMBULANCE",
                StatutAffectation.CONFIRMEE, 0.87, "Score élevé",
                "u-OPERATEUR", Instant.now(), Instant.now()
        );
        Mockito.when(affectationService.confirmerAffectation(Mockito.eq(1L), anyString())).thenReturn(response);

        mockMvc.perform(patch("/api/dispatch/affectations/1/confirmer").with(as("OPERATEUR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("CONFIRMEE"));
    }

    @Test
    @DisplayName("GET /affectations avec ADMIN → 200 + liste vide")
    void listerAffectations_admin_200() throws Exception {
        Mockito.when(affectationService.listerAffectations(null)).thenReturn(List.of());

        mockMvc.perform(get("/api/dispatch/affectations").with(as("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 404 — Ressource introuvable
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /affectations/{id} avec id inconnu → 404")
    void getAffectation_inconnue_404() throws Exception {
        Mockito.when(affectationService.getAffectation(999L))
                .thenThrow(new NotFoundException("AFFECTATION_INTROUVABLE", "Affectation introuvable : id=999"));

        mockMvc.perform(get("/api/dispatch/affectations/999").with(as("OPERATEUR")))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 409 — Conflit
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /affectations/{id}/confirmer sur affectation déjà CONFIRMEE → 409")
    void confirmerAffectation_dejaConfirmee_409() throws Exception {
        Mockito.when(affectationService.confirmerAffectation(Mockito.eq(1L), anyString()))
                .thenThrow(new ConflictException("TRANSITION_INVALIDE",
                        "L'affectation 1 est déjà CONFIRMEE"));

        mockMvc.perform(patch("/api/dispatch/affectations/1/confirmer").with(as("OPERATEUR")))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /affectations avec ressource déjà affectée → 409")
    void creerAffectation_ressourceDejaAffectee_409() throws Exception {
        AffectationRequest request = new AffectationRequest("INC-001", "RES-OCCUPEE", "AMBULANCE");
        Mockito.when(affectationService.creerAffectation(any(), anyString()))
                .thenThrow(new ConflictException("RESSOURCE_DEJA_AFFECTEE",
                        "La ressource RES-OCCUPEE est déjà affectée"));

        mockMvc.perform(post("/api/dispatch/affectations")
                        .with(as("OPERATEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 400 — Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /affectations avec body invalide (champs vides) → 400")
    void creerAffectation_bodyInvalide_400() throws Exception {
        String bodyInvalide = "{\"incidentId\": \"\", \"ressourceId\": \"\"}";

        mockMvc.perform(post("/api/dispatch/affectations")
                        .with(as("OPERATEUR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyInvalide))
                .andExpect(status().isBadRequest());
    }
}
