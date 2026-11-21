package tn.esprit.rescueops.hospital.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.rescueops.hospital.domain.DemandeTransfert;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Chaque transition est un UPDATE conditionné par le statut attendu : si deux actions arrivent en même temps
 * (ex. refus et expiration), une seule touche la ligne, l'autre reçoit 0 et échoue proprement.
 */
public interface DemandeTransfertRepository extends JpaRepository<DemandeTransfert, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DemandeTransfert d SET d.statut = :nouveau, d.traiteParUserId = :acteur,
                   d.motifRefus = COALESCE(:motif, d.motifRefus), d.updatedAt = CURRENT_TIMESTAMP
            WHERE d.id = :id AND d.statut = :attendu
            """)
    int changerStatut(@Param("id") UUID id, @Param("attendu") StatutTransfert attendu,
                      @Param("nouveau") StatutTransfert nouveau, @Param("acteur") String acteur,
                      @Param("motif") String motif);

    /** DEMANDEE → PLACE_RETENUE en associant le service et l'échéance. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DemandeTransfert d SET d.statut = tn.esprit.rescueops.hospital.domain.StatutTransfert.PLACE_RETENUE,
                   d.service.id = :serviceId, d.expireAt = :expireAt, d.traiteParUserId = :acteur,
                   d.updatedAt = :maintenant
            WHERE d.id = :id AND d.statut = tn.esprit.rescueops.hospital.domain.StatutTransfert.DEMANDEE
            """)
    int retenirPlace(@Param("id") UUID id, @Param("serviceId") UUID serviceId, @Param("expireAt") Instant expireAt,
                     @Param("acteur") String acteur, @Param("maintenant") Instant maintenant);

    /** PLACE_RETENUE → EXPIREE seulement si l'échéance est réellement passée. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE DemandeTransfert d SET d.statut = tn.esprit.rescueops.hospital.domain.StatutTransfert.EXPIREE,
                   d.updatedAt = :maintenant
            WHERE d.id = :id AND d.statut = tn.esprit.rescueops.hospital.domain.StatutTransfert.PLACE_RETENUE
              AND d.expireAt < :maintenant
            """)
    int expirer(@Param("id") UUID id, @Param("maintenant") Instant maintenant);

    @Query("""
            SELECT d.id FROM DemandeTransfert d
            WHERE d.statut = tn.esprit.rescueops.hospital.domain.StatutTransfert.PLACE_RETENUE AND d.expireAt < :maintenant
            ORDER BY d.expireAt
            """)
    List<UUID> idsEchus(@Param("maintenant") Instant maintenant, Pageable limite);

    Page<DemandeTransfert> findByStatut(StatutTransfert statut, Pageable pageable);

    List<DemandeTransfert> findByMissionIdAndStatutIn(UUID missionId, List<StatutTransfert> statuts);

    long countByStatut(StatutTransfert statut);

    @Query("""
            SELECT d.service.id, COUNT(d) FROM DemandeTransfert d
            WHERE d.statut = tn.esprit.rescueops.hospital.domain.StatutTransfert.PLACE_RETENUE GROUP BY d.service.id
            """)
    List<Object[]> reservationsParService();
}
