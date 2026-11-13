package tn.esprit.rescueops.hospital.repository;

import tn.esprit.rescueops.hospital.model.ServiceHospitalier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ServiceHospitalierRepository extends JpaRepository<ServiceHospitalier, UUID> {
}
