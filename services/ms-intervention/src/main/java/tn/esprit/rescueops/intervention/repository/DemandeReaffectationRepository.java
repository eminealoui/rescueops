
package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.esprit.rescueops.intervention.domain.DemandeReaffectation;
import tn.esprit.rescueops.intervention.domain.StatutReaffectation;

import java.util.List;
import java.util.UUID;

public interface DemandeReaffectationRepository
    extends JpaRepository<DemandeReaffectation, UUID> {

    List<DemandeReaffectation>
    findByMission_IdOrderByDateDemandeDesc(UUID missionId);

    boolean existsByMission_IdAndStatut(
        UUID missionId,
        StatutReaffectation statut
    );

}
