package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.intervention.domain.Intervention;

public interface InterventionRepository extends JpaRepository<Intervention, String> {
}
