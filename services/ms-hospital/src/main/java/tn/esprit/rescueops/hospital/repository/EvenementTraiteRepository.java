package tn.esprit.rescueops.hospital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.hospital.domain.EvenementTraite;

import java.util.UUID;

public interface EvenementTraiteRepository extends JpaRepository<EvenementTraite, UUID> {
}
