
package tn.esprit.rescueops.intervention.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.repository.MissionRepository;

import java.time.Instant;
import java.util.UUID;

@Service
public class MissionAutomatiqueService {

    private final MissionRepository missionRepository;

    public MissionAutomatiqueService(
        MissionRepository missionRepository
    ) {
        this.missionRepository = missionRepository;
    }

    @Transactional
    public Mission creerDepuisAffectation(
        UUID affectationId,
        UUID incidentId,
        UUID ambulanceId,
        String ambulancierKeycloakId
    ) {

        // 1. Éviter plusieurs missions pour une affectation
        var missionExistante = missionRepository
            .findByAffectationId(affectationId);

        if (missionExistante.isPresent()) {
            return missionExistante.get();
        }

        // 2. Créer automatiquement la mission
        Mission mission = new Mission();

        mission.setAffectationId(affectationId);
        mission.setIncidentId(incidentId);
        mission.setAmbulanceId(ambulanceId);
        mission.setAmbulancierKeycloakId(
            ambulancierKeycloakId
        );

        // 3. Initialiser la mission
        mission.setStatut(StatutMission.ACCEPTEE);

        mission.setCommentaire(
            "Mission créée automatiquement depuis l'affectation "
                + affectationId
        );

        // Une création RabbitMQ ne possède pas de JWT utilisateur
        mission.setCreePar("SYSTEM_DISPATCH");

        mission.setCreeLe(Instant.now());

        // 4. Enregistrer dans MySQL
        return missionRepository.save(mission);
    }
}
