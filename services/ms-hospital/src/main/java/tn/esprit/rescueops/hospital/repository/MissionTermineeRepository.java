package tn.esprit.rescueops.hospital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.hospital.domain.MissionTerminee;

import java.util.UUID;

public interface MissionTermineeRepository extends JpaRepository<MissionTerminee, UUID> {
}
