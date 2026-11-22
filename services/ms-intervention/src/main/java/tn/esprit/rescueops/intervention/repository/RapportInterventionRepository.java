
package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.intervention.domain.RapportIntervention;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RapportInterventionRepository
    extends JpaRepository<RapportIntervention, UUID> {

    Optional<RapportIntervention> findByMission_Id(UUID missionId);

    boolean existsByMission_Id(UUID missionId);

    @Query("""
    SELECT r FROM RapportIntervention r
    JOIN r.mission m
    WHERE m.ambulancierKeycloakId = :userId
       OR (
           m.ambulancierKeycloakId IS NULL
           AND m.creePar = :userId
       )
    """)
    Page<RapportIntervention> findPourUtilisateur(
        @Param("userId") String userId,
        Pageable pageable
    );

}
