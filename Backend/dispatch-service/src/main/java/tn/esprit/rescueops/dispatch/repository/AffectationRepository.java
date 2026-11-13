package tn.esprit.rescueops.dispatch.repository;

import tn.esprit.rescueops.dispatch.model.Affectation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AffectationRepository extends JpaRepository<Affectation, UUID> {
}
