package tn.esprit.rescueops.hospital.repository;

import tn.esprit.rescueops.hospital.model.Hopital;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface HopitalRepository extends JpaRepository<Hopital, UUID> {
}
