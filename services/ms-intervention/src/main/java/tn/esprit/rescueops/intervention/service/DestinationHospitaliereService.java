package tn.esprit.rescueops.intervention.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.error.ConflictException;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.domain.TypeItineraire;
import tn.esprit.rescueops.intervention.repository.MissionRepository;

import java.time.Instant;
import java.util.UUID;

@Service
public class DestinationHospitaliereService {

    private final MissionRepository missionRepository;

    public DestinationHospitaliereService(
        MissionRepository missionRepository
    ) {
        this.missionRepository = missionRepository;
    }

    @Transactional
    public void confirmerDestination(
        UUID missionId,
        UUID transfertId,
        UUID serviceHospitalierId,
        Instant dateConfirmation
    ) {

        Mission mission = missionRepository
            .findByIdForUpdate(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // Ignorer les confirmations anciennes ou deja appliquees
        if (mission.getDestinationConfirmeeLe() != null
            && !dateConfirmation.isAfter(
            mission.getDestinationConfirmeeLe())) {
            return;
        }

        // Interdire la modification d'une mission terminee
        if (mission.getStatut() == StatutMission.TERMINEE) {
            throw new ConflictException(
                "La mission est deja terminee"
            );
        }

        mission.setTransfertId(transfertId);
        mission.setServiceHospitalierId(serviceHospitalierId);
        mission.setDestinationConfirmeeLe(dateConfirmation);

        if (mission.getTypeItineraire()
            == TypeItineraire.VERS_HOPITAL) {

            mission.setTypeItineraire(null);

            mission.setLatitudeDepart(null);
            mission.setLongitudeDepart(null);

            mission.setLatitudeDestination(null);
            mission.setLongitudeDestination(null);

            mission.setDistanceKm(null);
            mission.setDureeEstimeeMinutes(null);

            mission.setItineraireModifieLe(null);
        }


        missionRepository.save(mission);
    }
}
