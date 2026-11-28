package tn.esprit.rescueops.emergency.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.emergency.domain.HistoriqueIncident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeActionIncident;
import tn.esprit.rescueops.emergency.dto.HistoriqueIncidentResponse;
import tn.esprit.rescueops.emergency.repository.HistoriqueIncidentRepository;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;

@Service
public class HistoriqueIncidentService {

    private final HistoriqueIncidentRepository repository;

    private final IncidentRepository incidentRepository;

    public HistoriqueIncidentService(
            HistoriqueIncidentRepository repository,
            IncidentRepository incidentRepository
    ) {
        this.repository = repository;
        this.incidentRepository = incidentRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void enregistrer(
            UUID incidentId,
            TypeActionIncident action,
            StatutIncident ancienStatut,
            StatutIncident nouveauStatut,
            String auteur,
            String details
    ) {

        HistoriqueIncident historique =
                new HistoriqueIncident();

        historique.setIncidentId(incidentId);
        historique.setAction(action);
        historique.setAncienStatut(ancienStatut);
        historique.setNouveauStatut(nouveauStatut);
        historique.setAuteur(auteur);
        historique.setDetails(details);
        historique.setDateAction(Instant.now());

        repository.save(historique);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueIncidentResponse> consulter(
            UUID incidentId
    ) {

        if (!incidentRepository.existsById(incidentId)) {

            throw new NotFoundException(
                    "Incident introuvable"
            );
        }

        return repository
                .findByIncidentIdOrderByDateActionAscIdAsc(
                        incidentId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private HistoriqueIncidentResponse toResponse(
            HistoriqueIncident historique
    ) {

        return new HistoriqueIncidentResponse(
                historique.getId(),
                historique.getIncidentId(),
                historique.getAction(),
                historique.getAncienStatut(),
                historique.getNouveauStatut(),
                historique.getAuteur(),
                historique.getDetails(),
                historique.getDateAction()
        );
    }
}