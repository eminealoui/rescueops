
package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.intervention.domain.Mission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.Optional;
import java.util.UUID;

public interface MissionRepository
    extends JpaRepository<Mission, UUID> {

    // Vérifier si une affectation possède déjà une mission
    boolean existsByAffectationId(UUID affectationId);

    // Rechercher une mission grâce à son affectation
    Optional<Mission> findByAffectationId(UUID affectationId);

    @Query("""
    SELECT m FROM Mission m
    WHERE m.ambulancierKeycloakId = :userId
       OR (
           m.ambulancierKeycloakId IS NULL
           AND m.creePar = :userId
       )
    """)
    Page<Mission> findPourUtilisateur(
        @Param("userId") String userId,
        Pageable pageable
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Mission m WHERE m.id = :id")
    Optional<Mission> findByIdForUpdate(@Param("id") UUID id);

}
