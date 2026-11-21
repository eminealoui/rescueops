package tn.esprit.rescueops.hospital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.hospital.domain.HistoriqueCapacite;

import java.util.UUID;

public interface HistoriqueCapaciteRepository extends JpaRepository<HistoriqueCapacite, UUID> {
}
