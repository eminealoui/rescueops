package tn.esprit.rescueops.emergency.service;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.emergency.domain.EvenementTraite;
import tn.esprit.rescueops.emergency.domain.Incident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.repository.EvenementTraiteRepository;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;

class AffectationEventServiceTest {

    private final IncidentRepository incidentRepository =
            mock(IncidentRepository.class);

    private final EvenementTraiteRepository evenementRepository =
            mock(EvenementTraiteRepository.class);

    private final IncidentService incidentService =
            mock(IncidentService.class);

    private final AffectationEventService service =
            new AffectationEventService(
                    incidentRepository,
                    evenementRepository,
                    incidentService
            );

    private Incident creerIncident(StatutIncident statut) {

        Incident incident = new Incident();

        incident.setId(UUID.randomUUID());
        incident.setStatut(statut);

        when(incidentRepository.trouverPourModification(
                incident.getId()
        )).thenReturn(Optional.of(incident));

        return incident;
    }

    @Test
    void doitPasserIncidentEnCours() {

        Incident incident =
                creerIncident(StatutIncident.VALIDE);

        UUID eventId = UUID.randomUUID();

        when(evenementRepository.existsById(eventId))
                .thenReturn(false);

        service.traiter(
                eventId,
                incident.getId()
        );

        verify(incidentService)
                .mettreEnCours(incident.getId());

        verify(evenementRepository)
                .saveAndFlush(any(EvenementTraite.class));
    }

    @Test
    void doitIgnorerEvenementDejaTraite() {

        Incident incident =
                creerIncident(StatutIncident.EN_COURS);

        UUID eventId = UUID.randomUUID();

        when(evenementRepository.existsById(eventId))
                .thenReturn(true);

        service.traiter(
                eventId,
                incident.getId()
        );

        verifyNoInteractions(incidentService);

        verify(evenementRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void doitRefuserIncidentNonValide() {

        Incident incident =
                creerIncident(StatutIncident.SIGNALE);

        UUID eventId = UUID.randomUUID();

        assertThrows(
                BusinessRuleException.class,
                () -> service.traiter(
                        eventId,
                        incident.getId()
                )
        );

        verifyNoInteractions(incidentService);
    }
}