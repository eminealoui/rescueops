
package tn.esprit.rescueops.intervention.service;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.dto.MissionRequest;
import tn.esprit.rescueops.intervention.dto.MissionResponse;
import tn.esprit.rescueops.intervention.repository.MissionRepository;
import tn.esprit.rescueops.intervention.dto.ItineraireRequest;
import tn.esprit.rescueops.intervention.dto.ItineraireResponse;
import tn.esprit.rescueops.intervention.domain.TypeItineraire;
import tn.esprit.rescueops.intervention.event.MissionEventPublisher;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import org.springframework.security.access.AccessDeniedException;
import tn.esprit.rescueops.intervention.dto.ModifierMissionRequest;
import tn.esprit.rescueops.common.error.ConflictException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tn.esprit.rescueops.intervention.domain.HistoriqueStatutMission;
import tn.esprit.rescueops.intervention.repository.HistoriqueStatutMissionRepository;

import org.springframework.security.access.AccessDeniedException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;


import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;

import java.util.List;
import tn.esprit.rescueops.intervention.dto.HistoriqueMissionResponse;

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
    private final CalculItineraireService calculItineraireService;

    public MissionService(
        MissionRepository repository,
        HistoriqueStatutMissionRepository historiqueRepository,
        MissionEventPublisher eventPublisher,
        CalculItineraireService calculItineraireService
    ) {
        this.repository = repository;
        this.historiqueRepository = historiqueRepository;
        this.eventPublisher = eventPublisher;
        this.calculItineraireService = calculItineraireService;
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
        verifierLectureMission(mission);

        // 3. Retourner les informations de la mission
        return toResponse(mission);
    }


    // Convertir Mission en MissionResponse
    private MissionResponse toResponse(Mission mission) {

        return new MissionResponse(

            mission.getId(),
            mission.getCommentaire(),
            mission.getStatut(),
            mission.getCreeLe(),

            mission.getAffectationId(),
            mission.getIncidentId(),
            mission.getAmbulanceId(),
            mission.getAmbulancierKeycloakId(),

            mission.getTransfertId(),
            mission.getServiceHospitalierId(),
            mission.getDestinationConfirmeeLe()
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

    private void verifierLectureMission(Mission mission) {

        CurrentUser utilisateur = CurrentUser.current()
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Authentification requise"
                )
            );

        // Administrateur et operateur :
        // consultation autorisee
        if (utilisateur.has(Role.ADMIN)
            || utilisateur.has(Role.OPERATEUR)) {
            return;
        }

        // Ambulancier :
        // uniquement ses missions
        verifierAccesMission(mission);
    }



    @Transactional(readOnly = true)
    public Page<MissionResponse> lister(Pageable pageable) {

        CurrentUser utilisateur = CurrentUser.current()
            .orElseThrow(() ->
                new AccessDeniedException(
                    "Authentification requise"
                )
            );

        Page<Mission> missions;

        if (utilisateur.has(Role.ADMIN)
            || utilisateur.has(Role.OPERATEUR)) {

            // Voir toutes les missions
            missions = repository.findAll(pageable);

        } else {

            // Voir uniquement les missions affectees
            missions = repository.findPourUtilisateur(
                utilisateur.keycloakId(),
                pageable
            );
        }

        return missions.map(this::toResponse);
    }



    @Transactional
    public MissionResponse modifier(
        UUID id,
        ModifierMissionRequest request
    ) {

        // 1. Chercher la mission
        Mission mission = repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 2. Vérifier les droits de l'ambulancier
        verifierAccesMission(mission);

        // 3. Protéger les missions déjà terminées
        if (mission.getStatut() == StatutMission.TERMINEE) {
            throw new ConflictException(
                "Une mission terminée ne peut plus être modifiée"
            );
        }

        // 4. Modifier uniquement le commentaire
        mission.setCommentaire(request.commentaire());

        // 5. Enregistrer dans MySQL
        Mission missionModifiee = repository.save(mission);

        return toResponse(missionModifiee);
    }

    @Transactional
    public void supprimer(UUID id) {

        // 1. Rechercher la mission
        Mission mission = repository.findById(id)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 2. Vérifier les droits d'accès
        verifierAccesMission(mission);

        // 3. Protéger les missions provenant de Dispatch
        if (mission.getAffectationId() != null) {
            throw new ConflictException(
                "Une mission affectée par Dispatch ne peut pas être supprimée"
            );
        }

        // 4. Autoriser uniquement les missions non commencées
        if (mission.getStatut() != StatutMission.ACCEPTEE) {
            throw new ConflictException(
                "Seule une mission non commencée peut être supprimée"
            );
        }

        // 5. Vérifier l'absence d'historique
        if (!historiqueRepository
            .findByMission_IdOrderByDateChangementAsc(id)
            .isEmpty()) {

            throw new ConflictException(
                "Une mission avec historique ne peut pas être supprimée"
            );
        }

        // 6. Supprimer la mission
        repository.delete(mission);
    }

    @Transactional(readOnly = true)
    public List<HistoriqueMissionResponse> historique(UUID missionId) {

        // 1. Chercher la mission
        Mission mission = repository.findById(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        // 2. Vérifier les permissions
        verifierLectureMission(mission);

        // 3. Récupérer la chronologie des statuts
        return historiqueRepository
            .findByMission_IdOrderByDateChangementAsc(missionId)
            .stream()
            .map(h -> new HistoriqueMissionResponse(
                h.getId(),
                String.valueOf(h.getAncienStatut()),
                String.valueOf(h.getNouveauStatut()),
                h.getDateChangement(),
                h.getModifiePar()
            ))
            .toList();
    }
    @Transactional
    public ItineraireResponse modifierItineraire(
        UUID missionId,
        ItineraireRequest request
    ) {

        Mission mission = repository.findByIdForUpdate(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        verifierAccesMission(mission);

        if (mission.getStatut() == StatutMission.TERMINEE) {
            throw new ConflictException(
                "Une mission terminee ne peut pas etre modifiee"
            );
        }

        if (request.typeItineraire() == TypeItineraire.VERS_HOPITAL
            && mission.getServiceHospitalierId() == null) {

            throw new ConflictException(
                "Aucune destination hospitaliere confirmee"
            );
        }

        mission.setTypeItineraire(request.typeItineraire());

        mission.setLatitudeDepart(
            request.latitudeDepart().doubleValue());

        mission.setLongitudeDepart(
            request.longitudeDepart().doubleValue());

        mission.setLatitudeDestination(
            request.latitudeDestination().doubleValue());

        mission.setLongitudeDestination(
            request.longitudeDestination().doubleValue());


        CalculItineraireService.Estimation estimation =
            calculItineraireService.calculer(
                request.latitudeDepart(),
                request.longitudeDepart(),
                request.latitudeDestination(),
                request.longitudeDestination()
            );

        mission.setDistanceKm(estimation.distanceKm());

        mission.setDureeEstimeeMinutes(
            estimation.dureeEstimeeMinutes()
        );


        mission.setItineraireModifieLe(Instant.now());

        return toItineraireResponse(repository.save(mission));
    }
    @Transactional(readOnly = true)
    public ItineraireResponse trouverItineraire(UUID missionId) {

        Mission mission = repository.findById(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        verifierLectureMission(mission);

        if (mission.getTypeItineraire() == null) {
            throw new NotFoundException(
                "Aucun itineraire disponible"
            );
        }

        return toItineraireResponse(mission);
    }
    @Transactional
    public void supprimerItineraire(UUID missionId) {

        Mission mission = repository.findByIdForUpdate(missionId)
            .orElseThrow(() ->
                new NotFoundException("Mission introuvable")
            );

        verifierAccesMission(mission);

        if (mission.getStatut() == StatutMission.TERMINEE) {
            throw new ConflictException(
                "Une mission terminee ne peut pas etre modifiee"
            );
        }

        mission.setTypeItineraire(null);

        mission.setLatitudeDepart(null);
        mission.setLongitudeDepart(null);

        mission.setLatitudeDestination(null);
        mission.setLongitudeDestination(null);

        mission.setDistanceKm(null);
        mission.setDureeEstimeeMinutes(null);
        mission.setItineraireModifieLe(null);

        repository.save(mission);
    }
    private ItineraireResponse toItineraireResponse(
        Mission mission
    ) {

        return new ItineraireResponse(
            mission.getId(),
            mission.getTypeItineraire(),
            mission.getLatitudeDepart(),
            mission.getLongitudeDepart(),
            mission.getLatitudeDestination(),
            mission.getLongitudeDestination(),
            mission.getDistanceKm(),
            mission.getDureeEstimeeMinutes(),
            mission.getItineraireModifieLe()
        );
    }





}
