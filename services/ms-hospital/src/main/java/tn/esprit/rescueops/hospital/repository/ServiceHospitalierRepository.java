package tn.esprit.rescueops.hospital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;

import java.util.List;
import java.util.UUID;

/**
 * La capacité ne change QUE par ces requêtes atomiques : la base décide, en une seule instruction,
 * s'il reste une place. Deux réservations simultanées ne peuvent donc jamais prendre la même (BNF-05).
 */
public interface ServiceHospitalierRepository extends JpaRepository<ServiceHospitalier, UUID> {

    /** Prend une place si le service et son hôpital sont actifs et qu'il en reste. @return 1 si réussi, 0 sinon. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServiceHospitalier s SET s.placesDisponibles = s.placesDisponibles - 1
            WHERE s.id = :id AND s.actif = true AND s.placesDisponibles > 0
              AND s.hopital.id IN (SELECT h.id FROM Hopital h WHERE h.actif = true)
            """)
    int reserverPlace(@Param("id") UUID serviceId);

    /** Rend une place, sans jamais dépasser la capacité. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServiceHospitalier s SET s.placesDisponibles = s.placesDisponibles + 1
            WHERE s.id = :id AND s.placesDisponibles < s.capaciteTotale
            """)
    int rendrePlace(@Param("id") UUID serviceId);

    /** Ajustement manuel « comparer puis écrire » : 0 si une réservation a modifié les valeurs entre-temps. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServiceHospitalier s SET s.capaciteTotale = :nouvelleCapacite, s.placesDisponibles = :nouvellesPlaces
            WHERE s.id = :id AND s.capaciteTotale = :ancienneCapacite AND s.placesDisponibles = :anciennesPlaces
            """)
    int ajusterCapacite(@Param("id") UUID serviceId, @Param("ancienneCapacite") int ancienneCapacite,
                        @Param("anciennesPlaces") int anciennesPlaces, @Param("nouvelleCapacite") int nouvelleCapacite,
                        @Param("nouvellesPlaces") int nouvellesPlaces);

    @Query("SELECT s FROM ServiceHospitalier s JOIN FETCH s.hopital h WHERE s.actif = true AND h.actif = true")
    List<ServiceHospitalier> findActifsAvecHopital();

    @Query("SELECT s FROM ServiceHospitalier s JOIN FETCH s.hopital h")
    List<ServiceHospitalier> findTousAvecHopital();
}
