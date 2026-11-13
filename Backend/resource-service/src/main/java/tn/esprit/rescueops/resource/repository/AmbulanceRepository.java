package tn.esprit.rescueops.resource.repository;

import tn.esprit.rescueops.resource.model.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AmbulanceRepository extends JpaRepository<Ambulance, UUID> {
}
