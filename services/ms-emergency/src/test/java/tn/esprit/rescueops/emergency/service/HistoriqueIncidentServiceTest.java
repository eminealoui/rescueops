package tn.esprit.rescueops.emergency.service;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.emergency.domain.HistoriqueIncident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeActionIncident;
import tn.esprit.rescueops.emergency.repository.HistoriqueIncidentRepository;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;

class HistoriqueIncidentServiceTest {

    private final HistoriqueIncidentRepository repository =
            mock(HistoriqueIncidentRepository.class);

    private final IncidentRepository incidentRepository =
            mock(IncidentRepository.class);

    private final HistoriqueIncidentService service =
            new HistoriqueIncidentService(
                    repository,
                    incidentRepository
            );

    @Test
    void doitEnregistrerCreation() {

        UUID id = UUID.randomUUID();

        service.enregistrer(
                id,
                TypeActionIncident.CREATION,
                null,
                StatutIncident.SIGNALE,
                "utilisateur-test",
                "Nouveau signalement"
        );

        ArgumentCaptor<HistoriqueIncident> captor =
                ArgumentCaptor.forClass(
                        HistoriqueIncident.class
                );

        verify(repository).save(captor.capture());

        HistoriqueIncident historique =
                captor.getValue();

        assertEquals(id, historique.getIncidentId());

        assertEquals(
                TypeActionIncident.CREATION,
                historique.getAction()
        );

        assertNull(historique.getAncienStatut());

        assertEquals(
                StatutIncident.SIGNALE,
                historique.getNouveauStatut()
        );

        assertNotNull(historique.getDateAction());
    }

    @Test
    void doitRefuserIncidentInexistant() {

        UUID id = UUID.randomUUID();

        when(incidentRepository.existsById(id))
                .thenReturn(false);

        assertThrows(
                NotFoundException.class,
                () -> service.consulter(id)
        );

        verifyNoInteractions(repository);
    }
}