
package tn.esprit.rescueops.intervention.service;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.dto.MissionRequest;
import tn.esprit.rescueops.intervention.dto.MissionResponse;
import tn.esprit.rescueops.intervention.repository.MissionRepository;

import tn.esprit.rescueops.intervention.event.MissionEventPublisher;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tn.esprit.rescueops.intervention.domain.HistoriqueStatutMission;
import tn.esprit.rescueops.intervention.repository.HistoriqueStatutMissionRepository;

import org.springframework.security.access.AccessDeniedException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;


import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.intervention.dto.ChangerStatutMissionRequest;
import tn.esprit.rescueops.common.error.ConflictException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;


import java.time.Instant;
import java.util.UUID;

@Service
public class MissionService {



    private final MissionRepository repository;
    private final HistoriqueStatutMissionRepository historiqueRepository;
    private final MissionEventPublisher eventPublisher;

    public MissionService(
        MissionRepository repository,
        HistoriqueStatutMissionRepository historiqueRepository,
        MissionEventPublisher eventPublisher
    ) {
        this.repository = repository;
        this.historiqueRepository = historiqueRepository;
        this.eventPublisher = eventPublisher;
    }



    // Créer une mission
    @Transactional
    public MissionResponse creer(MissionRequest request) {

        Mission mission = new Mission();

        mission.setCommentaire(request.commentaire());
        mission.setStatut(StatutMission.ACCEPTEE);

        mission.setCreePar(
            CurrentUser.current()
                .orElseThrow()
                .keycloakId()
        );

        mission.setCreeLe(Instant.now());

        return toResponse(repository.save(mission));
    }

    // Rechercher une mission par ID

    @Transactional(readOnly = true)
    public MissionResponse trouver(UUID id) {

        // 1. Récupérer la mission depuis MySQL
        Mission mission = repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 2. Vérifier que l'utilisateur a le droit de la consulter
        verifierAccesMission(mission);

        // 3. Retourner les informations de la mission
        return toResponse(mission);
    }


    // Convertir Mission en MissionResponse
    private MissionResponse toResponse(Mission mission) {

        return new MissionResponse(
            mission.getId(),
            mission.getCommentaire(),
            mission.getStatut(),
            mission.getCreeLe()
        );
    }



    @Transactional
    public MissionResponse changerStatut(
        UUID id,
        ChangerStatutMissionRequest request
    ) {
        // 1. Rechercher la mission
        Mission mission = repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable"));

        // 2. Récupérer l'utilisateur connecté
        String utilisateur = CurrentUser.current()
            .orElseThrow()
            .keycloakId();

        // 3. Vérifier si l'utilisateur est administrateur
        boolean admin = SecurityContextHolder.getContext()
            .getAuthentication()
            .getAuthorities()
            .stream()
            .anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN"));


        boolean autorise;

        if (mission.getAmbulancierKeycloakId() != null) {
            // Mission créée automatiquement depuis Dispatch
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


        // 4. Vérifier la transition de statut
        StatutMission ancienStatut = mission.getStatut();

        StatutMission prochainStatut = switch (ancienStatut) {
            case ACCEPTEE -> StatutMission.EN_ROUTE;
            case EN_ROUTE -> StatutMission.SUR_PLACE;
            case SUR_PLACE -> StatutMission.TERMINEE;
            case TERMINEE -> throw new ConflictException(
                "La mission est déjà terminée");
        };

        if (request.statut() != prochainStatut) {
            throw new ConflictException(
                "Transition de statut non autorisée");
        }

        // 5. Modifier le statut dans MySQL
        mission.setStatut(prochainStatut);
        repository.save(mission);

        // 6. Enregistrer l'historique
        HistoriqueStatutMission historique =
            new HistoriqueStatutMission();

        historique.setMission(mission);
        historique.setAncienStatut(ancienStatut);
        historique.setNouveauStatut(prochainStatut);
        historique.setDateChangement(Instant.now());
        historique.setModifiePar(utilisateur);

        historiqueRepository.save(historique);

        // 7. Préparer les données des événements
        UUID missionId = mission.getId();
        String ancien = ancienStatut.name();
        String nouveau = prochainStatut.name();

        // 8. Publier les événements après le commit MySQL
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {

                @Override
                public void afterCommit() {

                    // Publier chaque changement de statut
                    eventPublisher.publierChangementStatut(
                        missionId,
                        ancien,
                        nouveau
                    );

                    // Publier également la fin de mission
                    if ("TERMINEE".equals(nouveau)) {
                        eventPublisher.publierMissionTerminee(
                            missionId
                        );
                    }
                }
            }
        );

        // 9. Retourner la mission
        return toResponse(mission);
    }


    private void verifierAccesMission(Mission mission) {

        CurrentUser utilisateur = CurrentUser.current()
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Authentification requise"
                )
            );

        // Un administrateur peut consulter toutes les missions
        if (utilisateur.has(Role.ADMIN)) {
            return;
        }

        // Mission automatique : vérifier l'ambulancier affecté
        String proprietaire = mission.getAmbulancierKeycloakId();

        // Ancienne mission manuelle : vérifier le créateur
        if (proprietaire == null) {
            proprietaire = mission.getCreePar();
        }

        if (!utilisateur.keycloakId().equals(proprietaire)) {
            throw new AccessDeniedException(
                "Vous n'êtes pas autorisé à consulter cette mission"
            );
        }
    }


}
