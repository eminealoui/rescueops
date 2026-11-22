
package tn.esprit.rescueops.intervention.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;

import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.intervention.event.MissionEventPublisher;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;
import tn.esprit.rescueops.intervention.domain.*;
import tn.esprit.rescueops.intervention.dto.*;
import tn.esprit.rescueops.intervention.repository.*;
import java.util.List;
import tn.esprit.rescueops.common.security.Role;
import java.time.Instant;
import java.util.UUID;

@Service
public class DemandeReaffectationService {

    private final DemandeReaffectationRepository repository;
    private final MissionRepository missionRepository;
    private final MissionEventPublisher eventPublisher;

    public DemandeReaffectationService(
        DemandeReaffectationRepository repository,
        MissionRepository missionRepository,
        MissionEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.missionRepository = missionRepository;
        this.eventPublisher = eventPublisher;
    }


    @Transactional
    public DemandeReaffectationResponse creer(
        UUID missionId,
        DemandeReaffectationRequest request
    ) {

        // 1. Récupérer l'utilisateur connecté
        CurrentUser utilisateur = CurrentUser.current()
            .orElseThrow();

        // 2. Rechercher et verrouiller la mission
        Mission mission = missionRepository
            .findByIdForUpdate(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 3. Vérifier les droits d'accès
        if (!utilisateur.keycloakId().equals(
            mission.getAmbulancierKeycloakId())) {

            throw new AccessDeniedException(
                "Vous n'êtes pas affecté à cette mission"
            );
        }

        // 4. Vérifier l'affectation
        if (mission.getAffectationId() == null) {
            throw new ConflictException(
                "Cette mission ne possède pas d'affectation"
            );
        }

        // 5. Vérifier le statut de la mission
        if (mission.getStatut() == StatutMission.TERMINEE) {
            throw new ConflictException(
                "Une mission terminée ne peut pas être réaffectée"
            );
        }

        // 6. Vérifier l'absence de demande en attente
        if (repository.existsByMission_IdAndStatut(
            missionId,
            StatutReaffectation.EN_ATTENTE)) {

            throw new ConflictException(
                "Une demande de réaffectation est déjà en attente"
            );
        }

        // 7. Créer la demande
        DemandeReaffectation demande =
            new DemandeReaffectation();

        demande.setMission(mission);
        demande.setMotif(request.motif());
        demande.setStatut(StatutReaffectation.EN_ATTENTE);
        demande.setDemandePar(utilisateur.keycloakId());
        demande.setDateDemande(Instant.now());

        DemandeReaffectation saved = repository.save(demande);

        programmerEvenement(
            "mission.reaffectation.demandee",
            saved
        );

        return toResponse(saved);
    }

    private DemandeReaffectationResponse toResponse(
        DemandeReaffectation demande
    ) {
        return new DemandeReaffectationResponse(
            demande.getId(),
            demande.getMission().getId(),
            demande.getMotif(),
            demande.getStatut(),
            demande.getDemandePar(),
            demande.getDateDemande(),
            demande.getDateTraitement()
        );
    }

    private void verifierAcces(Mission mission) {

        CurrentUser utilisateur = CurrentUser.current()
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Authentification requise"
                )
            );

        if (utilisateur.has(Role.ADMIN)) {
            return;
        }

        if (!utilisateur.keycloakId().equals(
            mission.getAmbulancierKeycloakId())) {

            throw new AccessDeniedException(
                "Vous n'êtes pas autorisé à consulter cette mission"
            );
        }
    }

    @Transactional(readOnly = true)
    public DemandeReaffectationResponse trouver(UUID id) {

        DemandeReaffectation demande = repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Demande de réaffectation introuvable"
                )
            );

        verifierAcces(demande.getMission());

        return toResponse(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeReaffectationResponse> lister(
        UUID missionId
    ) {

        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        verifierAcces(mission);

        return repository
            .findByMission_IdOrderByDateDemandeDesc(missionId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public DemandeReaffectationResponse modifier(
        UUID id,
        DemandeReaffectationRequest request
    ) {

        DemandeReaffectation demande = repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Demande de reaffectation introuvable"
                )
            );

        verifierAcces(demande.getMission());

        if (demande.getStatut() != StatutReaffectation.EN_ATTENTE) {

            throw new ConflictException(
                "Seule une demande en attente peut etre modifiee"
            );
        }

        if (demande.getMission().getStatut() == StatutMission.TERMINEE) {

            throw new ConflictException(
                "La mission est deja terminee"
            );
        }

        demande.setMotif(request.motif());

        DemandeReaffectation saved = repository.save(demande);

        programmerEvenement(
            "mission.reaffectation.modifiee",
            saved
        );

        return toResponse(saved);
    }


    @Transactional
    public void annuler(UUID id) {

        DemandeReaffectation demande = repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException(
                    "Demande de reaffectation introuvable"
                )
            );

        verifierAcces(demande.getMission());

        if (demande.getStatut() != StatutReaffectation.EN_ATTENTE) {

            throw new ConflictException(
                "Seule une demande en attente peut etre annulee"
            );
        }

        demande.setStatut(StatutReaffectation.ANNULEE);
        demande.setDateTraitement(Instant.now());

        DemandeReaffectation saved = repository.save(demande);

        programmerEvenement(
            "mission.reaffectation.annulee",
            saved
        );
    }

    private void programmerEvenement(
        String eventType,
        DemandeReaffectation demande
    ) {
        System.out.println("[REAFFECTATION] Programmation : " + eventType);

        Mission mission = demande.getMission();

        Map<String, String> payload = new HashMap<>();

        payload.put("demandeId", demande.getId().toString());
        payload.put("missionId", mission.getId().toString());
        payload.put("motif", demande.getMotif());
        payload.put("statut", demande.getStatut().name());
        payload.put("demandePar", demande.getDemandePar());

        if (mission.getAffectationId() != null) {
            payload.put("affectationId",
                mission.getAffectationId().toString());
        }

        if (mission.getIncidentId() != null) {
            payload.put("incidentId",
                mission.getIncidentId().toString());
        }

        if (mission.getAmbulanceId() != null) {
            payload.put("ambulanceId",
                mission.getAmbulanceId().toString());
        }

        if (mission.getAmbulancierKeycloakId() != null) {
            payload.put("ambulancierKeycloakId",
                mission.getAmbulancierKeycloakId());
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {

                @Override
                public void afterCommit() {

                    System.out.println("[REAFFECTATION] Transaction validee");

                    eventPublisher.publierReaffectation(
                        eventType,
                        payload
                    );
                }
            }
        );
    }

    @Transactional
    public void traiterDecision(
        UUID demandeId,
        UUID missionId,
        UUID ancienneAffectationId,
        boolean acceptee,
        UUID nouvelleAffectationId,
        UUID nouvelleAmbulanceId,
        String nouvelAmbulancierKeycloakId
    ) {

        // 1. Rechercher et verrouiller la mission
        Mission mission = missionRepository
            .findByIdForUpdate(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 2. Rechercher la demande
        DemandeReaffectation demande =
            repository.findById(demandeId)
                .orElseThrow(() ->
                    new NotFoundException(
                        "Demande introuvable"
                    )
                );

        // 3. Verifier la relation avec la mission
        if (!demande.getMission().getId().equals(missionId)) {

            throw new ConflictException(
                "La demande ne correspond pas a cette mission"
            );
        }

        // 4. Ignorer les demandes deja traitees ou annulees
        if (demande.getStatut() != StatutReaffectation.EN_ATTENTE) {
            return;
        }

        // 5. Verifier le statut de la mission
        if (mission.getStatut() == StatutMission.TERMINEE) {

            throw new ConflictException(
                "La mission est deja terminee"
            );
        }

        // 6. Verifier que la decision concerne
        // bien l'affectation actuelle
        if (ancienneAffectationId == null ||
            !ancienneAffectationId.equals(
                mission.getAffectationId())) {

            throw new ConflictException(
                "L'affectation actuelle a change"
            );
        }

        // 7. Traiter la decision
        if (acceptee) {

            if (nouvelleAffectationId == null ||
                nouvelleAmbulanceId == null ||
                nouvelAmbulancierKeycloakId == null ||
                nouvelAmbulancierKeycloakId.isBlank()) {

                throw new ConflictException(
                    "Informations de reaffectation incompletes"
                );
            }

            mission.setAffectationId(nouvelleAffectationId);
            mission.setAmbulanceId(nouvelleAmbulanceId);

            mission.setAmbulancierKeycloakId(
                nouvelAmbulancierKeycloakId
            );

//
// Invalider l'ancien itineraire apres reaffectation
//
            mission.setTypeItineraire(null);

            mission.setLatitudeDepart(null);
            mission.setLongitudeDepart(null);

            mission.setLatitudeDestination(null);
            mission.setLongitudeDestination(null);

            mission.setDistanceKm(null);
            mission.setDureeEstimeeMinutes(null);

            mission.setItineraireModifieLe(null);


            missionRepository.save(mission);

            demande.setStatut(
                StatutReaffectation.ACCEPTEE
            );

        } else {

            demande.setStatut(
                StatutReaffectation.REFUSEE
            );
        }

        // 8. Enregistrer la date de traitement
        demande.setDateTraitement(Instant.now());

        repository.save(demande);
    }






}
