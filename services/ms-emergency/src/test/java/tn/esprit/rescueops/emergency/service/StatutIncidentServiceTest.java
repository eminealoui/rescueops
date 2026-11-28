package tn.esprit.rescueops.emergency.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.emergency.domain.StatutIncident;

class StatutIncidentServiceTest {

    private final StatutIncidentService service =
            new StatutIncidentService();

    @Test
    void doitAutoriserValidation() {

        assertDoesNotThrow(() ->
                service.verifierTransition(
                        StatutIncident.SIGNALE,
                        StatutIncident.VALIDE
                )
        );
    }

    @Test
    void doitAutoriserRejet() {

        assertDoesNotThrow(() ->
                service.verifierTransition(
                        StatutIncident.SIGNALE,
                        StatutIncident.REJETE
                )
        );
    }

    @Test
    void doitRefuserClotureDirecte() {

        assertThrows(
                BusinessRuleException.class,
                () -> service.verifierTransition(
                        StatutIncident.SIGNALE,
                        StatutIncident.CLOTURE
                )
        );
    }

    @Test
    void doitRefuserModificationIncidentRejete() {

        assertThrows(
                BusinessRuleException.class,
                () -> service.verifierTransition(
                        StatutIncident.REJETE,
                        StatutIncident.VALIDE
                )
        );
    }

    @Test
    void doitAutoriserTraitement() {

        assertDoesNotThrow(() ->
                service.verifierTransition(
                        StatutIncident.VALIDE,
                        StatutIncident.EN_COURS
                )
        );
    }

    @Test
    void doitAutoriserCloture() {

        assertDoesNotThrow(() ->
                service.verifierTransition(
                        StatutIncident.EN_COURS,
                        StatutIncident.CLOTURE
                )
        );
    }

    @Test
    void doitAutoriserFusion() {

        assertDoesNotThrow(() ->
                service.verifierTransition(
                        StatutIncident.SIGNALE,
                        StatutIncident.FUSIONNE
                )
        );
    }

    @Test
    void doitRefuserValidationIncidentFusionne() {

        assertThrows(
                BusinessRuleException.class,
                () -> service.verifierTransition(
                        StatutIncident.FUSIONNE,
                        StatutIncident.VALIDE
                )
        );
    }

    @Test
    void doitRefuserFusionIncidentValide() {

        assertThrows(
                BusinessRuleException.class,
                () -> service.verifierTransition(
                        StatutIncident.VALIDE,
                        StatutIncident.FUSIONNE
                )
        );
    }
}