package tn.esprit.rescueops.hospital.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.hospital.domain.Hopital;

import java.util.List;
import java.util.UUID;

public interface HopitalRepository extends JpaRepository<Hopital, UUID> {

    List<Hopital> findAllByOrderByNomAsc();
}
