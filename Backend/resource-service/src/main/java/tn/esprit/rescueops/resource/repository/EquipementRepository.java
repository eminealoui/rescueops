package tn.esprit.rescueops.resource.repository;

import tn.esprit.rescueops.resource.model.Equipement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EquipementRepository extends JpaRepository<Equipement, UUID> {
}
