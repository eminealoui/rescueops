package tn.esprit.rescueops.emergency.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import tn.esprit.rescueops.emergency.domain.Incident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeIncident;

public interface IncidentRepository
        extends JpaRepository<Incident, UUID> {

    Page<Incident> findByCreePar(
            String creePar,
            Pageable pageable
    );

    @Query("""
        select i from Incident i
        where (:statut is null or i.statut = :statut)
        and (:type is null or i.type = :type)
        and (
            :q is null
            or lower(i.titre) like lower(concat('%', :q, '%'))
            or lower(i.description) like lower(concat('%', :q, '%'))
            or lower(i.adresse) like lower(concat('%', :q, '%'))
        )
        """)
    Page<Incident> rechercher(
            @Param("statut") StatutIncident statut,
            @Param("type") TypeIncident type,
            @Param("q") String q,
            Pageable pageable
    );

    List<Incident>
    findByIdNotAndCreeLeBetweenAndStatutIn(
            UUID id,
            Instant debut,
            Instant fin,
            Collection<StatutIncident> statuts
    );

    boolean existsByIncidentPrincipalId(UUID id);

    List<Incident> findByIncidentPrincipalId(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Incident i where i.id = :id")
    Optional<Incident> trouverPourModification(
            @Param("id") UUID id
    );
}