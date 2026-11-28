package tn.esprit.rescueops.emergency.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tn.esprit.rescueops.emergency.domain.Incident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeIncident;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;

class DetectionDoublonServiceTest {

    private final IncidentRepository repository =
            mock(IncidentRepository.class);

    private final DetectionDoublonService service =
            new DetectionDoublonService(
                    repository,
                    new DistanceService(),
                    new SimilariteTexteService()
            );

    private Incident creerIncident(
            double latitude,
            double longitude
    ) {

        Incident incident = new Incident();

        incident.setId(UUID.randomUUID());
        incident.setTitre("Accident routier");
        incident.setDescription(
                "Accident entre deux voitures"
        );

        incident.setType(TypeIncident.ACCIDENT_ROUTIER);
        incident.setStatut(StatutIncident.SIGNALE);

        incident.setLatitude(latitude);
        incident.setLongitude(longitude);

        incident.setCreeLe(Instant.now());

        return incident;
    }

    @Test
    void doitDetecterDoublonProche() {

        Incident source = creerIncident(
                36.8065, 10.1815
        );

        Incident candidat = creerIncident(
                36.8067, 10.1816
        );

        when(repository.findById(source.getId()))
                .thenReturn(Optional.of(source));

        when(repository
                .findByIdNotAndCreeLeBetweenAndStatutIn(
                        any(), any(), any(), any()
                ))
                .thenReturn(List.of(candidat));

        var resultat = service.detecter(source.getId());

        assertEquals(1, resultat.size());

        assertEquals(
                candidat.getId(),
                resultat.get(0).incidentId()
        );

        assertEquals(
                100,
                resultat.get(0).scoreSimilarite()
        );
    }

    @Test
    void doitIgnorerIncidentEloigne() {

        Incident source = creerIncident(
                36.8065, 10.1815
        );

        Incident candidat = creerIncident(
                36.878, 10.3247
        );

        when(repository.findById(source.getId()))
                .thenReturn(Optional.of(source));

        when(repository
                .findByIdNotAndCreeLeBetweenAndStatutIn(
                        any(), any(), any(), any()
                ))
                .thenReturn(List.of(candidat));

        assertTrue(
                service.detecter(source.getId()).isEmpty()
        );
    }

    @Test
    void doitIgnorerIncidentFusionne() {

        Incident source = creerIncident(
                36.8065, 10.1815
        );

        source.setStatut(StatutIncident.FUSIONNE);

        when(repository.findById(source.getId()))
                .thenReturn(Optional.of(source));

        assertTrue(
                service.detecter(source.getId()).isEmpty()
        );
    }
}