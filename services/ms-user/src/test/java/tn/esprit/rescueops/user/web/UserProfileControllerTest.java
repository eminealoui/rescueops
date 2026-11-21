package tn.esprit.rescueops.user.web;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.user.dto.PageResponse;
import tn.esprit.rescueops.user.dto.UpdateMyProfileRequest;
import tn.esprit.rescueops.user.dto.UserProfileResponse;
import tn.esprit.rescueops.user.dto.UserSummaryDto;
import tn.esprit.rescueops.user.service.UserProfileService;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** API et règles d'accès, sans base de données (le service est simulé). */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration,"
                + "org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration,"
                + "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"})
@AutoConfigureMockMvc
class UserProfileControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserProfileService service;

    private static final UserProfileResponse PROFIL = new UserProfileResponse("sub-1", "a@rescueops.test", "Amine",
            "Ben Salah", null, null, null, true, Instant.parse("2026-11-21T10:00:00Z"), Instant.parse("2026-11-21T10:00:00Z"));

    private static RequestPostProcessor citoyen() {
        return jwt().jwt(j -> j.subject("sub-1").claim("email", "a@rescueops.test"))
                .authorities(new SimpleGrantedAuthority("ROLE_CITOYEN"));
    }

    private static RequestPostProcessor admin() {
        return jwt().jwt(j -> j.subject("sub-admin")).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Test
    void meSansToken401() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void meAvecTokenRenvoieLeProfil() throws Exception {
        when(service.getOrCreateCurrent(any(CurrentUser.class))).thenReturn(PROFIL);

        mvc.perform(get("/api/users/me").with(citoyen()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value("sub-1"))
                .andExpect(jsonPath("$.firstName").value("Amine"))
                .andExpect(jsonPath("$.id").doesNotExist());

        ArgumentCaptor<CurrentUser> user = ArgumentCaptor.forClass(CurrentUser.class);
        verify(service).getOrCreateCurrent(user.capture());
        assertThat(user.getValue().keycloakId()).isEqualTo("sub-1");
    }

    @Test
    void putMeIgnoreLesChampsInterdits() throws Exception {
        when(service.updateCurrent(any(), any())).thenReturn(PROFIL);

        mvc.perform(put("/api/users/me").with(citoyen()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"keycloakId\":\"pirate\",\"active\":false,\"roles\":[\"ADMIN\"]}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateMyProfileRequest> req = ArgumentCaptor.forClass(UpdateMyProfileRequest.class);
        ArgumentCaptor<CurrentUser> user = ArgumentCaptor.forClass(CurrentUser.class);
        verify(service).updateCurrent(user.capture(), req.capture());
        assertThat(req.getValue()).isEqualTo(new UpdateMyProfileRequest("A", "B", null, null));
        assertThat(user.getValue().keycloakId()).isEqualTo("sub-1");
    }

    @Test
    void putMePrenomVide400() throws Exception {
        mvc.perform(put("/api/users/me").with(citoyen()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"B\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void putMeTelephoneInvalide400() throws Exception {
        mvc.perform(put("/api/users/me").with(citoyen()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"A\",\"lastName\":\"B\",\"phone\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("phone"));
    }

    @Test
    void listeParCitoyen403() throws Exception {
        mvc.perform(get("/api/users").with(citoyen())).andExpect(status().isForbidden());
    }

    @Test
    void listeParAdminTailleBornee() throws Exception {
        when(service.search(any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(new PageResponse<>(List.of(PROFIL), 0, 100, 1, 1));

        mvc.perform(get("/api/users").param("size", "10000").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].keycloakId").value("sub-1"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).search(isNull(), isNull(), eq(0), eq(100), eq("lastName,asc"));
    }

    @Test
    void listeTriInconnu400() throws Exception {
        mvc.perform(get("/api/users").param("sort", "motDePasse,asc").with(admin()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRI_INVALIDE"));
    }

    @Test
    void statutParAdmin200() throws Exception {
        when(service.setActive("sub-1", false)).thenReturn(PROFIL);

        mvc.perform(patch("/api/users/sub-1/status").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());

        verify(service).setActive("sub-1", false);
    }

    @Test
    void statutParCitoyen403() throws Exception {
        mvc.perform(patch("/api/users/sub-1/status").with(citoyen()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
        verify(service, never()).setActive(any(), anyBoolean());
    }

    @Test
    void profilInconnu404() throws Exception {
        when(service.findByKeycloakId("inconnu")).thenThrow(new NotFoundException("Profil introuvable"));

        mvc.perform(get("/api/users/inconnu").with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"));
    }

    @Test
    void summaryAccessibleATousLesAuthentifies() throws Exception {
        when(service.summary("sub-1")).thenReturn(new UserSummaryDto("sub-1", "Amine Ben Salah", null));

        mvc.perform(get("/api/users/sub-1/summary").with(citoyen()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Amine Ben Salah"));
    }
}
