package tn.esprit.rescueops.training.repository;

import tn.esprit.rescueops.training.model.SessionFormation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface SessionFormationRepository extends JpaRepository<SessionFormation, UUID> {
}
