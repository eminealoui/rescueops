package tn.esprit.rescueops.common.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = GlobalExceptionHandlerTest.TestApp.class)
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(FailingController.class)
    static class TestApp {
    }

    record HopitalRequest(@NotBlank String nom) {
    }

    @RestController
    static class FailingController {
        @GetMapping("/not-found")
        void notFound() {
            throw new NotFoundException("Hôpital introuvable");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new ConflictException("CAPACITE_EPUISEE", "Plus aucune place disponible");
        }

        @GetMapping("/business")
        void business() {
            throw new BusinessRuleException("CAPACITE_INVALIDE", "Capacité inférieure aux places occupées");
        }

        @GetMapping("/bad-request")
        void badRequest() {
            throw new BadRequestException("TRI_INVALIDE", "Tri non autorisé : motDePasse");
        }

        @GetMapping("/forbidden-op")
        void forbiddenOp() {
            throw new ForbiddenOperationException("Seul le demandeur peut annuler");
        }

        @PostMapping("/validate")
        void validate(@Valid @RequestBody HopitalRequest req) {
        }

        @GetMapping("/sql")
        void sql() {
            throw new DataIntegrityViolationException("Duplicate entry 'x@y' for key 'uk_email'");
        }

        @GetMapping("/boom")
        void boom() {
            throw new IllegalStateException("boom secret interne");
        }
    }

    @Autowired
    MockMvc mvc;

    @Test
    void notFoundDonne404AvecCode() throws Exception {
        mvc.perform(get("/not-found").with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("RESSOURCE_INTROUVABLE"))
                .andExpect(jsonPath("$.detail").value("Hôpital introuvable"))
                .andExpect(jsonPath("$.instance").value("/not-found"))
                .andExpect(jsonPath("$.correlationId", not(emptyOrNullString())));
    }

    @Test
    void conflictDonne409AvecSonCode() throws Exception {
        mvc.perform(get("/conflict").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAPACITE_EPUISEE"));
    }

    @Test
    void regleMetierDonne422() throws Exception {
        mvc.perform(get("/business").with(jwt()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CAPACITE_INVALIDE"));
    }

    @Test
    void badRequestDonne400AvecSonCode() throws Exception {
        mvc.perform(get("/bad-request").with(jwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRI_INVALIDE"))
                .andExpect(jsonPath("$.detail").value("Tri non autorisé : motDePasse"));
    }

    @Test
    void operationInterditeDonne403() throws Exception {
        mvc.perform(get("/forbidden-op").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("OPERATION_INTERDITE"));
    }

    @Test
    void validationDonne400AvecLeChampEnErreur() throws Exception {
        mvc.perform(post("/validate").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{\"nom\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.errors[0].field").value("nom"))
                .andExpect(jsonPath("$.errors[0].message", not(emptyOrNullString())));
    }

    @Test
    void jsonIllisibleDonne400() throws Exception {
        mvc.perform(post("/validate").with(jwt()).contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_ILLISIBLE"));
    }

    @Test
    void violationSqlDonne409SansFuiteDeDetails() throws Exception {
        mvc.perform(get("/sql").with(jwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLIT_DONNEES"))
                .andExpect(content().string(not(containsString("Duplicate entry"))))
                .andExpect(content().string(not(containsString("uk_email"))));
    }

    @Test
    void erreurInattendueDonne500SansMessageInterne() throws Exception {
        mvc.perform(get("/boom").with(jwt()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERREUR_INTERNE"))
                .andExpect(content().string(not(containsString("boom secret"))));
    }

    @Test
    void correlationIdFourniEstRenvoyeEtPresentDansLErreur() throws Exception {
        mvc.perform(get("/not-found").with(jwt()).header("X-Correlation-Id", "abc12345-test"))
                .andExpect(header().string("X-Correlation-Id", "abc12345-test"))
                .andExpect(jsonPath("$.correlationId").value("abc12345-test"));
    }

    @Test
    void erreur401PorteAussiLeCorrelationId() throws Exception {
        mvc.perform(get("/not-found").header("X-Correlation-Id", "abc12345-anon"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").value("abc12345-anon"));
    }
}
