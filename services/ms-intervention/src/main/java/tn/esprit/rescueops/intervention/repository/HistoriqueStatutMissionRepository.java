
package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.intervention.domain.HistoriqueStatutMission;

import java.util.List;
import java.util.UUID;

public interface HistoriqueStatutMissionRepository
    extends JpaRepository<HistoriqueStatutMission, UUID> {

    List<HistoriqueStatutMission>
    findByMission_IdOrderByDateChangementAsc(UUID missionId);
}
