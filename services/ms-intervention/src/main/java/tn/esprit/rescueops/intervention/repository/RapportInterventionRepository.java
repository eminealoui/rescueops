
package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.intervention.domain.RapportIntervention;

import java.util.Optional;
import java.util.UUID;

public interface RapportInterventionRepository
    extends JpaRepository<RapportIntervention, UUID> {

    Optional<RapportIntervention> findByMission_Id(UUID missionId);

    boolean existsByMission_Id(UUID missionId);
}
