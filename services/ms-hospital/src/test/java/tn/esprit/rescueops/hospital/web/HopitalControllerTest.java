package tn.esprit.rescueops.hospital.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tn.esprit.rescueops.hospital.WebSliceTest;
import tn.esprit.rescueops.hospital.service.EtablissementService;
import tn.esprit.rescueops.hospital.service.TableauDeBordService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HopitalControllerTest extends ApiTestBase {

    static final String HOPITAL = """
            {"nom":"CHU La Rabta","adresse":"Bab Saadoun","ville":"Tunis","latitude":36.8065,"longitude":10.1647}""";




    static RequestPostProcessor as(String role) {
        return jwt().jwt(j -> j.subject("u-" + role)).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void creationParCitoyen403() throws Exception {
        mvc.perform(post("/api/hospitals").with(as("CITOYEN")).contentType(MediaType.APPLICATION_JSON).content(HOPITAL))
                .andExpect(status().isForbidden());
        verify(etablissements, never()).creerHopital(any());
    }

    @Test
    void creationParCoordinateur201() throws Exception {
        mvc.perform(post("/api/hospitals").with(as("COORDINATEUR_HOSPITALIER"))
                        .contentType(MediaType.APPLICATION_JSON).content(HOPITAL))
                .andExpect(status().isCreated());
        verify(etablissements).creerHopital(any());
    }

    @Test
    void latitudeHorsBornes400() throws Exception {
        mvc.perform(post("/api/hospitals").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content(HOPITAL.replace("36.8065", "95")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("latitude"));
    }

    @Test
    void lectureParCitoyen200() throws Exception {
        when(etablissements.listerHopitaux()).thenReturn(List.of());

        mvc.perform(get("/api/hospitals").with(as("CITOYEN"))).andExpect(status().isOk());
    }

    @Test
    void sansToken401() throws Exception {
        mvc.perform(get("/api/hospitals")).andExpect(status().isUnauthorized());
    }
}
