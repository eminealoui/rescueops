package tn.esprit.rescueops.emergency.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.esprit.rescueops.emergency.domain.HistoriqueIncident;

public interface HistoriqueIncidentRepository
        extends JpaRepository<HistoriqueIncident, UUID> {

    List<HistoriqueIncident>
        findByIncidentIdOrderByDateActionAscIdAsc(
            UUID incidentId
        );
}