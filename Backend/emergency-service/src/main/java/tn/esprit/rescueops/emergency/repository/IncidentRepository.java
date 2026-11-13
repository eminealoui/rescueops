package tn.esprit.rescueops.emergency.repository;

import tn.esprit.rescueops.emergency.model.Incident;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
}
