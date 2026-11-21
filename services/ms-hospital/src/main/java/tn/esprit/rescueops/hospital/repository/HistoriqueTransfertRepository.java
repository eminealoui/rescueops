package tn.esprit.rescueops.hospital.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.rescueops.hospital.domain.HistoriqueTransfert;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface HistoriqueTransfertRepository extends JpaRepository<HistoriqueTransfert, UUID> {

    List<HistoriqueTransfert> findByDemandeIdOrderByHorodatageAsc(UUID demandeId);

    /** Dernières transitions, pour le ticker « EN DIRECT ». */
    List<HistoriqueTransfert> findByHorodatageAfterOrderByHorodatageDesc(Instant depuis, Pageable limite);

    @Query("SELECT COUNT(h) FROM HistoriqueTransfert h WHERE h.nouveauStatut IN :statuts AND h.horodatage > :depuis")
    long compterDepuis(@Param("statuts") List<StatutTransfert> statuts, @Param("depuis") Instant depuis);
}
