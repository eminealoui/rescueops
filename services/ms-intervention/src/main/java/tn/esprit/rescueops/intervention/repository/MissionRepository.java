
package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.intervention.domain.Mission;

import java.util.Optional;
import java.util.UUID;

public interface MissionRepository
    extends JpaRepository<Mission, UUID> {

    // Vérifier si une affectation possède déjà une mission
    boolean existsByAffectationId(UUID affectationId);

    // Rechercher une mission grâce à son affectation
    Optional<Mission> findByAffectationId(UUID affectationId);
}
