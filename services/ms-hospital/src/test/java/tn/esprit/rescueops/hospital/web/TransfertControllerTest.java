package tn.esprit.rescueops.hospital.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.rescueops.hospital.WebSliceTest;
import tn.esprit.rescueops.hospital.service.TableauDeBordService;
import tn.esprit.rescueops.hospital.service.TransfertService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static tn.esprit.rescueops.hospital.web.HopitalControllerTest.as;

class TransfertControllerTest extends ApiTestBase {

    static final String DEMANDE = """
            {"missionId":"%s","specialiteRequise":"CARDIOLOGIE","gravite":"CRITIQUE","latitudeOrigine":36.8,"longitudeOrigine":10.18}"""
            .formatted(UUID.randomUUID());




    @Test
    void creationParCitoyen403() throws Exception {
        mvc.perform(post("/api/transfers").with(as("CITOYEN")).contentType(MediaType.APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isForbidden());
        verify(transferts, never()).creer(any(), any());
    }

    @Test
    void creationParAmbulancier201() throws Exception {
        mvc.perform(post("/api/transfers").with(as("AMBULANCIER")).contentType(MediaType.APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isCreated());
    }

    @Test
    void confirmationParCoordinateur200() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post("/api/transfers/{id}/confirm", id).with(as("COORDINATEUR_HOSPITALIER")))
                .andExpect(status().isOk());
        verify(transferts).confirmer(eq(id), any());
    }

    @Test
    void confirmationParAmbulancier403() throws Exception {
        mvc.perform(post("/api/transfers/{id}/confirm", UUID.randomUUID()).with(as("AMBULANCIER")))
                .andExpect(status().isForbidden());
        verify(transferts, never()).confirmer(any(), any());
    }

    @Test
    void reservationSansServiceId400() throws Exception {
        mvc.perform(post("/api/transfers/{id}/reserve", UUID.randomUUID()).with(as("AMBULANCIER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void compatiblesParAmbulancier200() throws Exception {
        mvc.perform(get("/api/hospitals/compatible").param("specialite", "CARDIOLOGIE").param("gravite", "CRITIQUE")
                        .param("lat", "36.8").param("lon", "10.18").with(as("AMBULANCIER")))
                .andExpect(status().isOk());
    }

    @Test
    void compatiblesParCitoyen403() throws Exception {
        mvc.perform(get("/api/hospitals/compatible").param("specialite", "CARDIOLOGIE").param("gravite", "CRITIQUE")
                        .with(as("CITOYEN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void fluxRefuseAuxCitoyens() throws Exception {
        mvc.perform(get("/api/transfers/feed").with(as("CITOYEN"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/transfers/feed").with(as("OPERATEUR"))).andExpect(status().isOk());
    }

    @Test
    void tableauDeBordLisibleParTousLesAuthentifies() throws Exception {
        mvc.perform(get("/api/hospitals/capacities").with(as("CITOYEN"))).andExpect(status().isOk());
    }
}
