package tn.esprit.rescueops.emergency.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.emergency.domain.Incident;
import tn.esprit.rescueops.emergency.domain.NiveauPriorite;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeActionIncident;
import tn.esprit.rescueops.emergency.domain.TypeIncident;
import tn.esprit.rescueops.emergency.dto.IncidentRequest;
import tn.esprit.rescueops.emergency.dto.IncidentResponse;
import tn.esprit.rescueops.emergency.dto.PageResponse;
import tn.esprit.rescueops.emergency.dto.RejetIncidentRequest;
import tn.esprit.rescueops.emergency.dto.ResultatPriorisation;
import tn.esprit.rescueops.emergency.dto.ValidationIncidentRequest;
import tn.esprit.rescueops.emergency.event.IncidentPrioriseEvent;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;


@Service
public class IncidentService {

    private final IncidentRepository repository;

    private final StatutIncidentService statutService;

    private final PriorisationService priorisationService; 

    private final HistoriqueIncidentService historiqueService;

    private final ApplicationEventPublisher eventPublisher;

    public IncidentService(
            IncidentRepository repository,
            StatutIncidentService statutService,
            PriorisationService priorisationService,
            HistoriqueIncidentService historiqueService,
            ApplicationEventPublisher eventPublisher
    ) {

        this.repository = repository;
        this.statutService = statutService;
        this.priorisationService = priorisationService;
        this.historiqueService = historiqueService;
        this.eventPublisher = eventPublisher;
    }

    // 1. Déclarer un incident

    @Transactional
    public IncidentResponse creer(IncidentRequest request) {

        Incident incident = new Incident();

        incident.setTitre(request.titre());
        incident.setDescription(request.description());
        incident.setType(request.type());
        incident.setAdresse(request.adresse());
        incident.setLatitude(request.latitude());
        incident.setLongitude(request.longitude());
        incident.setNombreVictimes(request.nombreVictimes());

        incident.setStatut(StatutIncident.SIGNALE);
        incident.setCreePar(utilisateurActuel());
        incident.setCreeLe(Instant.now());

        Incident saved = repository.saveAndFlush(incident);

        historiqueService.enregistrer(
                saved.getId(),
                TypeActionIncident.CREATION,
                null,
                StatutIncident.SIGNALE,
                utilisateurActuel(),
                "Nouveau signalement"
        );

        return toResponse(saved);    
    }

    // 2. Consulter un incident

    @Transactional(readOnly = true)
    public IncidentResponse trouver(UUID id) {

        Incident incident = chercherIncident(id);

        boolean responsable = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .anyMatch(role ->
                        role.getAuthority().equals("ROLE_OPERATEUR")
                        || role.getAuthority().equals("ROLE_ADMIN")
                );

        if (!responsable &&
                !incident.getCreePar().equals(utilisateurActuel())) {

            throw new AccessDeniedException("Accès interdit");
        }

        return toResponse(incident);
    }

    // 3. Rechercher les incidents

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> rechercher(
            StatutIncident statut,
            TypeIncident type,
            String q,
            int page,
            int size
    ) {

        String recherche = q == null || q.isBlank()
                ? null
                : q.trim();

        return PageResponse.from(
                repository.rechercher(
                        statut,
                        type,
                        recherche,
                        pagination(page, size)
                ).map(this::toResponse)
        );
    }

    // 4. Consulter mes incidents

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> mesIncidents(
            int page,
            int size
    ) {

        return PageResponse.from(
                repository.findByCreePar(
                        utilisateurActuel(),
                        pagination(page, size)
                ).map(this::toResponse)
        );
    }

    // 5. Valider et classifier un incident

    @Transactional
    public IncidentResponse valider(
            UUID id,
            ValidationIncidentRequest request
    ) {

    Incident incident = chercherIncident(id);

    statutService.verifierTransition(
            incident.getStatut(),
            StatutIncident.VALIDE
    );

    ResultatPriorisation resultat =
            priorisationService.calculer(
                    request.type(),
                    request.gravite(),
                    request.nombreVictimes(),
                    request.dangerImmediat(),
                    request.personnesVulnerables()
            );

    NiveauPriorite prioriteFinale =
            request.prioriteManuelle() == null
                    ? resultat.priorite()
                    : request.prioriteManuelle();

    String motif = request.motifAjustementPriorite();

    if (prioriteFinale != resultat.priorite()
            && (motif == null || motif.isBlank())) {

        throw new BusinessRuleException(
                "Un motif est obligatoire pour modifier la priorite suggeree"
        );
    }

    incident.setType(request.type());
    incident.setGravite(request.gravite());
    incident.setNombreVictimes(request.nombreVictimes());

    incident.setDangerImmediat(
            request.dangerImmediat()
    );

    incident.setPersonnesVulnerables(
            request.personnesVulnerables()
    );

    incident.setScorePriorite(resultat.score());

    incident.setPrioriteSuggeree(resultat.priorite());

    incident.setPriorite(prioriteFinale);

    incident.setExplicationPriorite(
            resultat.explication()
    );

    incident.setMotifAjustementPriorite(
            prioriteFinale != resultat.priorite()
                    ? motif.trim()
                    : null
    );

    incident.setStatut(StatutIncident.VALIDE);
    incident.setTraitePar(utilisateurActuel());
    incident.setTraiteLe(Instant.now());

    Incident saved = repository.save(incident);

    historiqueService.enregistrer(
            saved.getId(),
            TypeActionIncident.VALIDATION,
            StatutIncident.SIGNALE,
            StatutIncident.VALIDE,
            utilisateurActuel(),

            "Type : " + saved.getType()
            + " | Gravite : " + saved.getGravite()
            + " | Score : " + saved.getScorePriorite()
            + " | Priorite suggeree : "
            + saved.getPrioriteSuggeree()
            + " | Priorite finale : "
            + saved.getPriorite()
            + " | Motif ajustement : "
            + (saved.getMotifAjustementPriorite() == null
                ? "Aucun"
                : saved.getMotifAjustementPriorite())
    );

    eventPublisher.publishEvent(
        new IncidentPrioriseEvent(saved.getId())
    );

    return toResponse(saved);
}
    // 6. Rejeter un incident

    @Transactional
    public IncidentResponse rejeter(
            UUID id,
            RejetIncidentRequest request
    ) {

        Incident incident = chercherIncident(id);

        statutService.verifierTransition(
                incident.getStatut(),
                StatutIncident.REJETE
        );

        incident.setStatut(StatutIncident.REJETE);

        incident.setMotifRejet(request.motif().trim());

        incident.setTraitePar(utilisateurActuel());

        incident.setTraiteLe(Instant.now());

        Incident saved = repository.save(incident);

        historiqueService.enregistrer(
                saved.getId(),
                TypeActionIncident.REJET,
                StatutIncident.SIGNALE,
                StatutIncident.REJETE,
                utilisateurActuel(),
                saved.getMotifRejet()
        );

        return toResponse(saved);
    }

    // 7. Passer en cours de traitement
    // Cette méthode sera appelée par RabbitMQ plus tard.

    @Transactional
    public IncidentResponse mettreEnCours(UUID id) {

        Incident incident = chercherIncident(id);

        statutService.verifierTransition(
                incident.getStatut(),
                StatutIncident.EN_COURS
        );

        incident.setStatut(StatutIncident.EN_COURS);

        Incident saved = repository.save(incident);

        historiqueService.enregistrer(
                saved.getId(),
                TypeActionIncident.DEBUT_TRAITEMENT,
                StatutIncident.VALIDE,
                StatutIncident.EN_COURS,
                "SYSTEME:ms-dispatch",
                "Affectation des ressources confirmee"
        );

        return toResponse(saved);    }

    // 8. Clôturer un incident

    @Transactional
    public IncidentResponse cloturer(UUID id) {

        Incident incident = chercherIncident(id);

        statutService.verifierTransition(
                incident.getStatut(),
                StatutIncident.CLOTURE
        );

        incident.setStatut(StatutIncident.CLOTURE);

        incident.setCloturePar(utilisateurActuel());

        incident.setClotureLe(Instant.now());

        Incident saved = repository.save(incident);

        historiqueService.enregistrer(
                saved.getId(),
                TypeActionIncident.CLOTURE,
                StatutIncident.EN_COURS,
                StatutIncident.CLOTURE,
                utilisateurActuel(),
                "Incident cloture par un responsable"
        );

        return toResponse(saved);    }

    // Méthodes utilitaires

    private Incident chercherIncident(UUID id) {

        return repository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException("Incident introuvable")
                );
    }

    private Pageable pagination(int page, int size) {

        return PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(
                        Sort.Order.desc("creeLe"),
                        Sort.Order.desc("id")
                )
        );
    }

    private String utilisateurActuel() {

        return CurrentUser.current()
                .orElseThrow()
                .keycloakId();
    }

    private IncidentResponse toResponse(Incident incident) {

        return new IncidentResponse(
                incident.getId(),
                incident.getTitre(),
                incident.getDescription(),
                incident.getType(),
                incident.getAdresse(),
                incident.getLatitude(),
                incident.getLongitude(),
                incident.getNombreVictimes(),
                incident.getStatut(),
                incident.getCreeLe(),
                incident.getTraiteLe(),
                incident.getMotifRejet(),
                incident.getClotureLe(),

                incident.getGravite(),
                incident.getDangerImmediat(),
                incident.getPersonnesVulnerables(),
                incident.getScorePriorite(),
                incident.getPriorite(),
                incident.getPrioriteSuggeree(),
                incident.getExplicationPriorite(),
                incident.getMotifAjustementPriorite(),

                incident.getIncidentPrincipalId(),
                incident.getFusionnePar(),
                incident.getFusionneLe(),
                incident.getMotifFusion()
        );
    }
}