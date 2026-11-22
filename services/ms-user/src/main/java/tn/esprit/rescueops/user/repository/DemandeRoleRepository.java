package tn.esprit.rescueops.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.rescueops.user.domain.DemandeRole;
import tn.esprit.rescueops.user.domain.StatutDemandeRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DemandeRoleRepository extends JpaRepository<DemandeRole, UUID> {

    boolean existsByKeycloakIdAndStatut(String keycloakId, StatutDemandeRole statut);

    List<DemandeRole> findByKeycloakIdOrderByCreatedAtDesc(String keycloakId);

    Page<DemandeRole> findByStatut(StatutDemandeRole statut, Pageable pageable);

    /** EN_ATTENTE → décision, une seule fois : deux admins qui cliquent en même temps ne traitent pas deux fois. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DemandeRole d SET d.statut = :decision, d.traitePar = :admin, d.motifRefus = :motif,
                   d.traiteLe = :maintenant, d.version = d.version + 1
            WHERE d.id = :id AND d.statut = tn.esprit.rescueops.user.domain.StatutDemandeRole.EN_ATTENTE
            """)
    int cloturer(@Param("id") UUID id, @Param("decision") StatutDemandeRole decision, @Param("admin") String admin,
                 @Param("motif") String motif, @Param("maintenant") Instant maintenant);
}
