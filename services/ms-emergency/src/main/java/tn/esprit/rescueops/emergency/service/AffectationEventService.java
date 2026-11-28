package tn.esprit.rescueops.emergency.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.emergency.domain.*;
import tn.esprit.rescueops.emergency.repository.*;

import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.error.BusinessRuleException;

import java.time.Instant;
import java.util.UUID;

@Service
public class AffectationEventService {

    private final IncidentRepository incidentRepository;

    private final EvenementTraiteRepository evenementRepository;

    private final IncidentService incidentService;

    public AffectationEventService(

            IncidentRepository incidentRepository,

            EvenementTraiteRepository evenementRepository,

            IncidentService incidentService
    ) {

        this.incidentRepository = incidentRepository;
        this.evenementRepository = evenementRepository;
        this.incidentService = incidentService;
    }

    @Transactional
    public void traiter(
            UUID eventId,
            UUID incidentId
    ) {

        Incident incident =
                incidentRepository
                        .trouverPourModification(incidentId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Incident introuvable"
                                )
                        );

        // Ignorer les evenements deja traites

        if (evenementRepository.existsById(eventId)) {
            return;
        }

        // Modifier le statut seulement si necessaire

        if (incident.getStatut() == StatutIncident.VALIDE) {

            incidentService.mettreEnCours(incidentId);

        } else if (
                incident.getStatut() != StatutIncident.EN_COURS
                && incident.getStatut() != StatutIncident.CLOTURE
        ) {

            throw new BusinessRuleException(
                    "Incident non eligible a une affectation"
            );
        }

        // Memoriser l'evenement

        EvenementTraite evenement =
                new EvenementTraite();

        evenement.setEventId(eventId);

        evenement.setEventType(
                "affectation.confirmee"
        );

        evenement.setDateTraitement(
                Instant.now()
        );

        evenementRepository.saveAndFlush(evenement);
    }
}