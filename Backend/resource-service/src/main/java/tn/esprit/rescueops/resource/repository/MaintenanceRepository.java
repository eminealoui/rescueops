package tn.esprit.rescueops.resource.repository;

import tn.esprit.rescueops.resource.model.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MaintenanceRepository extends JpaRepository<Maintenance, UUID> {
}
