package tn.esprit.rescueops.hospital.repository;

import tn.esprit.rescueops.hospital.model.DemandeTransfert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DemandeTransfertRepository extends JpaRepository<DemandeTransfert, UUID> {
}
