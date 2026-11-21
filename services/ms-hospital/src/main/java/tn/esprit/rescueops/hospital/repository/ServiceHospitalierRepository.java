package tn.esprit.rescueops.hospital.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * La capacité ne change QUE par ces requêtes atomiques : la base décide, en une seule instruction,
 * s'il reste une place (BNF-05). Chacune incrémente aussi la version : toute écriture « entité entière »
 * lue avant elle échoue (verrouillage optimiste) au lieu d'écraser le compteur.
 */
public interface ServiceHospitalierRepository extends JpaRepository<ServiceHospitalier, UUID> {

    /** Prend une place si le service et son hôpital sont actifs et qu'il en reste. @return 1 si réussi, 0 sinon. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServiceHospitalier s SET s.placesDisponibles = s.placesDisponibles - 1, s.version = s.version + 1
            WHERE s.id = :id AND s.actif = true AND s.placesDisponibles > 0
              AND s.hopital.id IN (SELECT h.id FROM Hopital h WHERE h.actif = true)
            """)
    int reserverPlace(@Param("id") UUID serviceId);

    /** Rend une place, sans jamais dépasser la capacité. @return 0 signale une incohérence (journalisée). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServiceHospitalier s SET s.placesDisponibles = s.placesDisponibles + 1, s.version = s.version + 1
            WHERE s.id = :id AND s.placesDisponibles < s.capaciteTotale
            """)
    int rendrePlace(@Param("id") UUID serviceId);

    /** Ajustement manuel « comparer puis écrire » : 0 si une réservation a modifié les valeurs entre-temps. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServiceHospitalier s SET s.capaciteTotale = :nouvelleCapacite, s.placesDisponibles = :nouvellesPlaces,
                   s.version = s.version + 1
            WHERE s.id = :id AND s.capaciteTotale = :ancienneCapacite AND s.placesDisponibles = :anciennesPlaces
            """)
    int ajusterCapacite(@Param("id") UUID serviceId, @Param("ancienneCapacite") int ancienneCapacite,
                        @Param("anciennesPlaces") int anciennesPlaces, @Param("nouvelleCapacite") int nouvelleCapacite,
                        @Param("nouvellesPlaces") int nouvellesPlaces);

    /** Ouvre ou ferme un service SANS toucher au compteur de places. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ServiceHospitalier s SET s.actif = :actif, s.version = s.version + 1 WHERE s.id = :id")
    int changerActif(@Param("id") UUID serviceId, @Param("actif") boolean actif);

    /** Verrou exclusif sur la ligne (SELECT ... FOR UPDATE) : bloque les réservations le temps d'un ajustement. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ServiceHospitalier s WHERE s.id = :id")
    Optional<ServiceHospitalier> findByIdPourMiseAJour(@Param("id") UUID serviceId);

    @Query("SELECT s FROM ServiceHospitalier s JOIN FETCH s.hopital h WHERE s.actif = true AND h.actif = true")
    List<ServiceHospitalier> findActifsAvecHopital();

    @Query("SELECT s FROM ServiceHospitalier s JOIN FETCH s.hopital h")
    List<ServiceHospitalier> findTousAvecHopital();
}
