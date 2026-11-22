
package tn.esprit.rescueops.intervention.service;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.RapportIntervention;
import tn.esprit.rescueops.intervention.domain.StatutMission;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;

import tn.esprit.rescueops.intervention.dto.RapportRequest;
import tn.esprit.rescueops.intervention.dto.RapportResponse;

import tn.esprit.rescueops.intervention.repository.MissionRepository;
import tn.esprit.rescueops.intervention.repository.RapportInterventionRepository;

import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.security.CurrentUser;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RapportInterventionService {

    private final RapportInterventionRepository rapportRepository;
    private final MissionRepository missionRepository;

    public RapportInterventionService(
        RapportInterventionRepository rapportRepository,
        MissionRepository missionRepository
    ) {
        this.rapportRepository = rapportRepository;
        this.missionRepository = missionRepository;
    }

    // Créer un rapport d'intervention
    @Transactional
    public RapportResponse creer(
        UUID missionId,
        RapportRequest request
    ) {
        // 1. Vérifier que la mission existe
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable"));

        // 2. Vérifier les droits de l'utilisateur
        verifierAcces(mission);

        // 3. Vérifier que la mission est terminée
        if (mission.getStatut() != StatutMission.TERMINEE) {
            throw new ConflictException(
                "La mission doit être terminée pour créer un rapport"
            );
        }

        // 4. Empêcher deux rapports pour la même mission
        if (rapportRepository.existsByMission_Id(missionId)) {
            throw new ConflictException(
                "Un rapport existe déjà pour cette mission"
            );
        }

        // 5. Créer le rapport
        RapportIntervention rapport = new RapportIntervention();

        rapport.setMission(mission);
        rapport.setBilan(request.bilan());
        rapport.setActionsRealisees(request.actionsRealisees());
        rapport.setObservations(request.observations());

        rapport.setCreePar(
            CurrentUser.current()
                .orElseThrow()
                .keycloakId()
        );

        rapport.setCreeLe(Instant.now());

        // 6. Enregistrer dans MySQL
        rapport = rapportRepository.save(rapport);

        return toResponse(rapport);
    }

    // Consulter le rapport d'une mission
    @Transactional(readOnly = true)
    public RapportResponse trouver(UUID missionId) {

        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable"));

        verifierAcces(mission);

        RapportIntervention rapport = rapportRepository
            .findByMission_Id(missionId)
            .orElseThrow(() ->
                new NotFoundException("Rapport introuvable"));

        return toResponse(rapport);
    }

    // Vérifier les droits d'accès
    private void verifierAcces(Mission mission) {

        String utilisateur = CurrentUser.current()
            .orElseThrow()
            .keycloakId();

        boolean admin = SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN"));


        boolean autorise;

        if (mission.getAmbulancierKeycloakId() != null) {
            // Mission créée automatiquement par Dispatch
            autorise = mission.getAmbulancierKeycloakId()
                .equals(utilisateur);
        } else {
            // Ancienne mission créée manuellement
            autorise = mission.getCreePar().equals(utilisateur);
        }

        if (!admin && !autorise) {
            throw new AccessDeniedException(
                "Vous n'êtes pas affecté à cette mission"
            );
        }

    }

    // Convertir l'entité en DTO
    private RapportResponse toResponse(RapportIntervention rapport) {

        return new RapportResponse(
            rapport.getId(),
            rapport.getMission().getId(),
            rapport.getBilan(),
            rapport.getActionsRealisees(),
            rapport.getObservations(),
            rapport.getCreePar(),
            rapport.getCreeLe()
        );
    }

    @Transactional
    public RapportResponse modifier(
        UUID missionId,
        RapportRequest request
    ) {

        // 1. Rechercher la mission
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 2. Vérifier les droits d'accès
        verifierAcces(mission);

        // 3. Rechercher le rapport existant
        RapportIntervention rapport = rapportRepository
            .findByMission_Id(missionId)
            .orElseThrow(() ->
                new NotFoundException(
                    "Rapport introuvable"
                )
            );

        // 4. Modifier les informations du rapport
        rapport.setBilan(request.bilan());
        rapport.setActionsRealisees(request.actionsRealisees());
        rapport.setObservations(request.observations());

        // 5. Enregistrer dans MySQL
        RapportIntervention rapportModifie =
            rapportRepository.save(rapport);

        return toResponse(rapportModifie);
    }

    @Transactional
    public void supprimer(UUID missionId) {

        // 1. Rechercher la mission
        Mission mission = missionRepository.findById(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 2. Vérifier les droits d'accès
        verifierAcces(mission);

        // 3. Rechercher le rapport
        RapportIntervention rapport = rapportRepository
            .findByMission_Id(missionId)
            .orElseThrow(() ->
                new NotFoundException("Rapport introuvable")
            );

        // 4. Supprimer le rapport
        rapportRepository.delete(rapport);
    }

    @Transactional(readOnly = true)
    public Page<RapportResponse> lister(Pageable pageable) {

        CurrentUser utilisateur = CurrentUser.current()
            .orElseThrow();

        Page<RapportIntervention> rapports;

        if (utilisateur.has(Role.ADMIN)) {
            // Administrateur : tous les rapports
            rapports = rapportRepository.findAll(pageable);
        } else {
            // Ambulancier : uniquement ses rapports
            rapports = rapportRepository.findPourUtilisateur(
                utilisateur.keycloakId(),
                pageable
            );
        }

        return rapports.map(this::toResponse);
    }



}
