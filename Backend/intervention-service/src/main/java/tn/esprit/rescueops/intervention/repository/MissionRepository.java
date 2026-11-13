package tn.esprit.rescueops.intervention.repository;

import tn.esprit.rescueops.intervention.model.Mission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {
}
