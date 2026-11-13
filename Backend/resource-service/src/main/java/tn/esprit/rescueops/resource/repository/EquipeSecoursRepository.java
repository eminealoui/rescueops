package tn.esprit.rescueops.resource.repository;

import tn.esprit.rescueops.resource.model.EquipeSecours;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EquipeSecoursRepository extends JpaRepository<EquipeSecours, UUID> {
}
