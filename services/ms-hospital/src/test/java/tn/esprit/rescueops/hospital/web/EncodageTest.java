package tn.esprit.rescueops.hospital.web;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static tn.esprit.rescueops.hospital.web.HopitalControllerTest.as;

/**
 * PowerShell 5.1 (Invoke-RestMethod) décode en ISO-8859-1 une réponse sans charset :
 * « Hôpital » s'affichait « HÃ´pital ». Les réponses JSON doivent annoncer charset=UTF-8.
 */
class EncodageTest extends ApiTestBase {

    @Test
    void lesReponsesJsonAnnoncentUtf8() throws Exception {
        when(etablissements.listerHopitaux()).thenReturn(List.of());

        mvc.perform(get("/api/hospitals").with(as("CITOYEN")))
                .andExpect(header().string("Content-Type", containsStringIgnoringCase("charset=UTF-8")));
    }
}
