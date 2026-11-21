
package tn.esprit.rescueops.intervention.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.repository.MissionRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionAutomatiqueServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @InjectMocks
    private MissionAutomatiqueService service;

    @Test
    void creerMissionDepuisAffectation() {

        UUID affectationId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        UUID ambulanceId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        when(missionRepository.findByAffectationId(affectationId))
            .thenReturn(Optional.empty());

        when(missionRepository.save(any(Mission.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Mission mission = service.creerDepuisAffectation(
            affectationId,
            incidentId,
            ambulanceId,
            ambulancierId
        );

        assertNotNull(mission);
        assertEquals(StatutMission.ACCEPTEE, mission.getStatut());
        assertEquals(affectationId, mission.getAffectationId());
        assertEquals(incidentId, mission.getIncidentId());
        assertEquals(ambulanceId, mission.getAmbulanceId());
        assertEquals(ambulancierId,
            mission.getAmbulancierKeycloakId());

        verify(missionRepository, times(1))
            .save(any(Mission.class));
    }

    @Test
    void nePasCreerDeuxMissionsPourMemeAffectation() {

        UUID affectationId = UUID.randomUUID();

        Mission missionExistante = new Mission();
        missionExistante.setAffectationId(affectationId);
        missionExistante.setStatut(StatutMission.ACCEPTEE);

        when(missionRepository.findByAffectationId(affectationId))
            .thenReturn(Optional.of(missionExistante));

        Mission resultat = service.creerDepuisAffectation(
            affectationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID().toString()
        );

        assertSame(missionExistante, resultat);

        verify(missionRepository, never())
            .save(any(Mission.class));
    }
}
