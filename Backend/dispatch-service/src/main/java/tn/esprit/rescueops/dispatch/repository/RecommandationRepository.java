package tn.esprit.rescueops.dispatch.repository;

import tn.esprit.rescueops.dispatch.model.Recommandation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface RecommandationRepository extends JpaRepository<Recommandation, UUID> {
}
