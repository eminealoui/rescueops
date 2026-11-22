
package tn.esprit.rescueops.intervention.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import tn.esprit.rescueops.intervention.domain.TypeItineraire;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.intervention.domain.*;
import tn.esprit.rescueops.intervention.dto.DemandeReaffectationRequest;
import tn.esprit.rescueops.intervention.repository.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.rescueops.intervention.event.MissionEventPublisher;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandeReaffectationServiceTest {

    @Mock
    private DemandeReaffectationRepository repository;
    @Mock
    private MissionEventPublisher eventPublisher;

    @Mock
    private MissionRepository missionRepository;

    @InjectMocks
    private DemandeReaffectationService service;
    @BeforeEach
    void initialiserTransaction() {
        TransactionSynchronizationManager.initSynchronization();
    }

    private void connecterAmbulancier(String utilisateurId) {

        Jwt jwt = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject(utilisateurId)
            .claim("realm_access", Map.of(
                "roles", List.of("AMBULANCIER")
            ))
            .build();

        JwtAuthenticationToken authentication =
            new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority(
                    "ROLE_AMBULANCIER"
                ))
            );

        SecurityContextHolder.getContext()
            .setAuthentication(authentication);
    }

    @AfterEach
    void nettoyerContexte() {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }

        SecurityContextHolder.clearContext();
    }

    private Mission creerMission(String ambulancierId) {

        Mission mission = new Mission();

        mission.setId(UUID.randomUUID());
        mission.setAffectationId(UUID.randomUUID());
        mission.setAmbulancierKeycloakId(ambulancierId);
        mission.setStatut(StatutMission.ACCEPTEE);

        return mission;
    }

    @Test
    void creerDemandeReaffectation() {

        String userId = UUID.randomUUID().toString();
        connecterAmbulancier(userId);

        Mission mission = creerMission(userId);

        when(missionRepository.findByIdForUpdate(mission.getId()))
            .thenReturn(Optional.of(mission));

        when(repository.existsByMission_IdAndStatut(
            mission.getId(),
            StatutReaffectation.EN_ATTENTE))
            .thenReturn(false);

        when(repository.save(any(DemandeReaffectation.class)))
            .thenAnswer(invocation -> {
                DemandeReaffectation demande =
                    invocation.getArgument(0);

                demande.setId(UUID.randomUUID());

                return demande;
            });

        var resultat = service.creer(
            mission.getId(),
            new DemandeReaffectationRequest("Panne ambulance")
        );

        assertNotNull(resultat.id());
        assertEquals(mission.getId(), resultat.missionId());
        assertEquals("Panne ambulance", resultat.motif());
        assertEquals(StatutReaffectation.EN_ATTENTE,
            resultat.statut());
        assertEquals(userId, resultat.demandePar());
        assertNotNull(resultat.dateDemande());

        verify(repository).save(any(DemandeReaffectation.class));
    }

    @Test
    void refuserDemandeEnDouble() {

        String userId = UUID.randomUUID().toString();
        connecterAmbulancier(userId);

        Mission mission = creerMission(userId);

        when(missionRepository.findByIdForUpdate(mission.getId()))
            .thenReturn(Optional.of(mission));

        when(repository.existsByMission_IdAndStatut(
            mission.getId(),
            StatutReaffectation.EN_ATTENTE))
            .thenReturn(true);

        assertThrows(
            ConflictException.class,
            () -> service.creer(
                mission.getId(),
                new DemandeReaffectationRequest("Panne")
            )
        );

        verify(repository, never())
            .save(any(DemandeReaffectation.class));
    }

    @Test
    void refuserReaffectationMissionTerminee() {

        String userId = UUID.randomUUID().toString();
        connecterAmbulancier(userId);

        Mission mission = creerMission(userId);
        mission.setStatut(StatutMission.TERMINEE);

        when(missionRepository.findByIdForUpdate(mission.getId()))
            .thenReturn(Optional.of(mission));

        assertThrows(
            ConflictException.class,
            () -> service.creer(
                mission.getId(),
                new DemandeReaffectationRequest("Panne")
            )
        );

        verify(repository, never())
            .save(any(DemandeReaffectation.class));
    }

    @Test
    void refuserAutreAmbulancier() {

        String proprietaire = UUID.randomUUID().toString();
        String autreUtilisateur = UUID.randomUUID().toString();

        connecterAmbulancier(autreUtilisateur);

        Mission mission = creerMission(proprietaire);

        when(missionRepository.findByIdForUpdate(mission.getId()))
            .thenReturn(Optional.of(mission));

        assertThrows(
            AccessDeniedException.class,
            () -> service.creer(
                mission.getId(),
                new DemandeReaffectationRequest("Panne")
            )
        );

        verify(repository, never())
            .save(any(DemandeReaffectation.class));
    }

    @Test
    void modifierDemandeEnAttente() {

        String userId = UUID.randomUUID().toString();
        connecterAmbulancier(userId);

        Mission mission = creerMission(userId);

        DemandeReaffectation demande = new DemandeReaffectation();

        demande.setId(UUID.randomUUID());
        demande.setMission(mission);
        demande.setMotif("Ancien motif");
        demande.setStatut(StatutReaffectation.EN_ATTENTE);

        when(repository.findById(demande.getId()))
            .thenReturn(Optional.of(demande));

        when(repository.save(demande)).thenReturn(demande);

        var resultat = service.modifier(
            demande.getId(),
            new DemandeReaffectationRequest("Nouveau motif")
        );

        assertEquals("Nouveau motif", resultat.motif());
        assertEquals(StatutReaffectation.EN_ATTENTE,
            resultat.statut());

        verify(repository).save(demande);
    }


    @Test
    void annulerDemandeEnAttente() {

        String userId = UUID.randomUUID().toString();
        connecterAmbulancier(userId);

        Mission mission = creerMission(userId);

        DemandeReaffectation demande =
            new DemandeReaffectation();

        demande.setId(UUID.randomUUID());
        demande.setMission(mission);
        demande.setMotif("Panne ambulance");
        demande.setDemandePar(userId);
        demande.setStatut(StatutReaffectation.EN_ATTENTE);

        when(repository.findById(demande.getId()))
            .thenReturn(Optional.of(demande));

        // Correction du NullPointerException
        when(repository.save(demande))
            .thenReturn(demande);

        service.annuler(demande.getId());

        assertEquals(
            StatutReaffectation.ANNULEE,
            demande.getStatut()
        );

        assertNotNull(demande.getDateTraitement());

        verify(repository).save(demande);

        verify(repository, never()).delete(demande);

        // Verification de la publication apres commit
        verifyNoInteractions(eventPublisher);

        TransactionSynchronizationManager
            .getSynchronizations()
            .forEach(synchronization ->
                synchronization.afterCommit());

        verify(eventPublisher).publierReaffectation(
            eq("mission.reaffectation.annulee"),
            anyMap()
        );
    }


    @Test
    void refuserModificationDemandeAnnulee() {

        String userId = UUID.randomUUID().toString();
        connecterAmbulancier(userId);

        Mission mission = creerMission(userId);

        DemandeReaffectation demande = new DemandeReaffectation();

        demande.setId(UUID.randomUUID());
        demande.setMission(mission);
        demande.setStatut(StatutReaffectation.ANNULEE);

        when(repository.findById(demande.getId()))
            .thenReturn(Optional.of(demande));

        assertThrows(
            ConflictException.class,
            () -> service.modifier(
                demande.getId(),
                new DemandeReaffectationRequest("Motif")
            )
        );

        verify(repository, never())
            .save(any(DemandeReaffectation.class));
    }

    @Test
    void confirmationReaffectationInvalideItineraire() {

        String ancienAmbulancier = UUID.randomUUID().toString();
        String nouvelAmbulancier = UUID.randomUUID().toString();

        Mission mission = creerMission(ancienAmbulancier);

        UUID ancienneAffectation = mission.getAffectationId();
        UUID nouvelleAffectation = UUID.randomUUID();
        UUID nouvelleAmbulance = UUID.randomUUID();

        // La mission possede deja un itineraire
        mission.setTypeItineraire(TypeItineraire.VERS_INCIDENT);
        mission.setLatitudeDepart(36.8065);
        mission.setLongitudeDepart(10.1815);
        mission.setLatitudeDestination(36.8189);
        mission.setLongitudeDestination(10.1658);
        mission.setDistanceKm(2.67);
        mission.setDureeEstimeeMinutes(5);
        mission.setItineraireModifieLe(Instant.now());

        DemandeReaffectation demande =
            new DemandeReaffectation();

        demande.setId(UUID.randomUUID());
        demande.setMission(mission);
        demande.setStatut(StatutReaffectation.EN_ATTENTE);

        when(missionRepository.findByIdForUpdate(mission.getId()))
            .thenReturn(Optional.of(mission));

        when(repository.findById(demande.getId()))
            .thenReturn(Optional.of(demande));

        // Dispatch confirme la reaffectation
        service.traiterDecision(
            demande.getId(),
            mission.getId(),
            ancienneAffectation,
            true,
            nouvelleAffectation,
            nouvelleAmbulance,
            nouvelAmbulancier
        );

        // Verifier la nouvelle affectation
        assertEquals(nouvelleAffectation,
            mission.getAffectationId());

        assertEquals(nouvelleAmbulance,
            mission.getAmbulanceId());

        assertEquals(nouvelAmbulancier,
            mission.getAmbulancierKeycloakId());

        // Verifier que l'ancien itineraire est supprime
        assertNull(mission.getTypeItineraire());
        assertNull(mission.getLatitudeDepart());
        assertNull(mission.getLongitudeDepart());
        assertNull(mission.getLatitudeDestination());
        assertNull(mission.getLongitudeDestination());
        assertNull(mission.getDistanceKm());
        assertNull(mission.getDureeEstimeeMinutes());
        assertNull(mission.getItineraireModifieLe());

        // Verifier la decision
        assertEquals(StatutReaffectation.ACCEPTEE,
            demande.getStatut());

        assertNotNull(demande.getDateTraitement());

        verify(missionRepository).save(mission);
        verify(repository).save(demande);
    }


}
