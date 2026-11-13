package tn.esprit.rescueops.training.repository;

import tn.esprit.rescueops.training.model.Formation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FormationRepository extends JpaRepository<Formation, UUID> {
}
