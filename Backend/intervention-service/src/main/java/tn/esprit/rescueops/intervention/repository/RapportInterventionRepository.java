package tn.esprit.rescueops.intervention.repository;

import tn.esprit.rescueops.intervention.model.RapportIntervention;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RapportInterventionRepository extends JpaRepository<RapportIntervention, UUID> {
}
