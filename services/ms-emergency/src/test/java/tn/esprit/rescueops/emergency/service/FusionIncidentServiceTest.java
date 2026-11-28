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
import tn.esprit.rescueops.emergency.domain.Incident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.dto.FusionIncidentRequest;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;

class FusionIncidentServiceTest {

    private final IncidentRepository repository =
            mock(IncidentRepository.class);

    private final HistoriqueIncidentService historiqueService =
            mock(HistoriqueIncidentService.class);

    private final FusionIncidentService service =
            new FusionIncidentService(
                    repository,
                    new StatutIncidentService(),
                    historiqueService
            );

    private Incident creerIncident(StatutIncident statut) {

        Incident incident = new Incident();

        incident.setId(UUID.randomUUID());
        incident.setStatut(statut);

        return incident;
    }

    @Test
    void doitRefuserFusionAvecSoiMeme() {

        UUID id = UUID.randomUUID();

        var request = new FusionIncidentRequest(
                id,
                "Meme incident"
        );

        assertThrows(
                BusinessRuleException.class,
                () -> service.fusionner(id, request)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void doitRefuserIncidentSecondaireValide() {

        Incident principal =
                creerIncident(StatutIncident.SIGNALE);

        Incident secondaire =
                creerIncident(StatutIncident.VALIDE);

        when(repository.trouverPourModification(
                principal.getId()
        )).thenReturn(Optional.of(principal));

        when(repository.trouverPourModification(
                secondaire.getId()
        )).thenReturn(Optional.of(secondaire));

        var request = new FusionIncidentRequest(
                secondaire.getId(),
                "Doublon confirme"
        );

        assertThrows(
                BusinessRuleException.class,
                () -> service.fusionner(
                        principal.getId(),
                        request
                )
        );

        verify(repository, never()).save(any());
    }

    @Test
    void doitRefuserFusionEnCascade() {

        Incident principal =
                creerIncident(StatutIncident.SIGNALE);

        Incident secondaire =
                creerIncident(StatutIncident.SIGNALE);

        when(repository.trouverPourModification(
                principal.getId()
        )).thenReturn(Optional.of(principal));

        when(repository.trouverPourModification(
                secondaire.getId()
        )).thenReturn(Optional.of(secondaire));

        when(repository.existsByIncidentPrincipalId(
                secondaire.getId()
        )).thenReturn(true);

        var request = new FusionIncidentRequest(
                secondaire.getId(),
                "Doublon confirme"
        );

        assertThrows(
                BusinessRuleException.class,
                () -> service.fusionner(
                        principal.getId(),
                        request
                )
        );
    }
}