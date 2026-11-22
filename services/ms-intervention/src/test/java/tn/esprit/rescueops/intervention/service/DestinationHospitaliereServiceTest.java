
package tn.esprit.rescueops.intervention.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.domain.TypeItineraire;
import tn.esprit.rescueops.intervention.repository.MissionRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DestinationHospitaliereServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @InjectMocks
    private DestinationHospitaliereService service;

    private Mission creerMissionAvecItineraire(
        TypeItineraire type
    ) {

        Mission mission = new Mission();

        mission.setId(UUID.randomUUID());
        mission.setStatut(StatutMission.ACCEPTEE);

        mission.setTypeItineraire(type);

        mission.setLatitudeDepart(36.8065);
        mission.setLongitudeDepart(10.1815);

        mission.setLatitudeDestination(36.8189);
        mission.setLongitudeDestination(10.1658);

        mission.setDistanceKm(2.67);
        mission.setDureeEstimeeMinutes(5);
        mission.setItineraireModifieLe(Instant.now());

        return mission;
    }

    @Test
    void changementHopitalInvalideItineraireHospitalier() {

        Mission mission = creerMissionAvecItineraire(
            TypeItineraire.VERS_HOPITAL
        );

        UUID transfertId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        Instant confirmation = Instant.now();

        when(missionRepository.findByIdForUpdate(mission.getId()))
            .thenReturn(Optional.of(mission));

        service.confirmerDestination(
            mission.getId(),
            transfertId,
            serviceId,
            confirmation
        );

        // Nouvelle destination enregistree
        assertEquals(transfertId, mission.getTransfertId());

        assertEquals(serviceId,
            mission.getServiceHospitalierId());

        assertEquals(confirmation,
            mission.getDestinationConfirmeeLe());

        // Ancien itineraire vers l'hopital invalide
        assertNull(mission.getTypeItineraire());
        assertNull(mission.getLatitudeDepart());
        assertNull(mission.getLongitudeDepart());
        assertNull(mission.getLatitudeDestination());
        assertNull(mission.getLongitudeDestination());
        assertNull(mission.getDistanceKm());
        assertNull(mission.getDureeEstimeeMinutes());

        verify(missionRepository).save(mission);
    }

    @Test
    void changementHopitalConserveItineraireVersIncident() {

        Mission mission = creerMissionAvecItineraire(
            TypeItineraire.VERS_INCIDENT
        );

        when(missionRepository.findByIdForUpdate(mission.getId()))
            .thenReturn(Optional.of(mission));

        service.confirmerDestination(
            mission.getId(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            Instant.now()
        );

        // Le trajet vers l'incident reste valide
        assertEquals(
            TypeItineraire.VERS_INCIDENT,
            mission.getTypeItineraire()
        );

        assertEquals(2.67, mission.getDistanceKm(), 0.001);
        assertEquals(5, mission.getDureeEstimeeMinutes());

        verify(missionRepository).save(mission);
    }
}
