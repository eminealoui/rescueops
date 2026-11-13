package tn.esprit.rescueops.training.repository;

import tn.esprit.rescueops.training.model.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CertificationRepository extends JpaRepository<Certification, UUID> {
}
