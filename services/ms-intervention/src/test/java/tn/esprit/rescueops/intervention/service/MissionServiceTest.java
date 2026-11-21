
package tn.esprit.rescueops.intervention.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import tn.esprit.rescueops.intervention.dto.ChangerStatutMissionRequest;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import tn.esprit.rescueops.intervention.dto.ChangerStatutMissionRequest;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tn.esprit.rescueops.intervention.dto.ChangerStatutMissionRequest;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.rescueops.common.error.ConflictException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.dto.ChangerStatutMissionRequest;
import tn.esprit.rescueops.intervention.event.MissionEventPublisher;
import tn.esprit.rescueops.intervention.repository.HistoriqueStatutMissionRepository;
import tn.esprit.rescueops.intervention.repository.MissionRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private HistoriqueStatutMissionRepository historiqueRepository;

    @Mock
    private MissionEventPublisher eventPublisher;

    @InjectMocks
    private MissionService service;

    @AfterEach
    void nettoyerSecurite() {
        SecurityContextHolder.clearContext();
    }

    private void connecterAmbulancier(String utilisateurId) {

        Jwt jwt = Jwt.withTokenValue("token-test")
            .header("alg", "none")
            .subject(utilisateurId)
            .claim("realm_access",
                Map.of("roles", List.of("AMBULANCIER")))
            .build();

        AbstractAuthenticationToken auth =
            new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority(
                    "ROLE_AMBULANCIER"
                ))
            );

        SecurityContextHolder.getContext()
            .setAuthentication(auth);
    }

    @Test
    void ambulancierAffectePeutConsulterSaMission() {

        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        connecterAmbulancier(ambulancierId);

        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.ACCEPTEE);
        mission.setCommentaire("Mission de test");
        mission.setCreeLe(Instant.now());
        mission.setAmbulancierKeycloakId(ambulancierId);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        var resultat = service.trouver(missionId);

        assertNotNull(resultat);
        assertEquals(missionId, resultat.id());
    }

    @Test
    void autreAmbulancierNePeutPasConsulterLaMission() {

        UUID missionId = UUID.randomUUID();

        String ambulancierAffecte = UUID.randomUUID().toString();
        String autreAmbulancier = UUID.randomUUID().toString();

        connecterAmbulancier(autreAmbulancier);

        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setAmbulancierKeycloakId(ambulancierAffecte);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        assertThrows(
            AccessDeniedException.class,
            () -> service.trouver(missionId)
        );
    }

    @Test
    void refuserTransitionDirecteAccepteeVersTerminee() {

        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        // 1. Simuler l'ambulancier connecté
        connecterAmbulancier(ambulancierId);

        // 2. Préparer une mission au statut ACCEPTEE
        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.ACCEPTEE);
        mission.setAmbulancierKeycloakId(ambulancierId);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        // 3. Tenter une transition interdite
        assertThrows(
            tn.esprit.rescueops.common.error.ConflictException.class,
            () -> service.changerStatut(
                missionId,
                new ChangerStatutMissionRequest(
                    StatutMission.TERMINEE
                )
            )
        );

        // 4. Vérifier qu'aucune modification n'a été enregistrée
        verify(missionRepository, never())
            .save(any(Mission.class));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void accepterTransitionAccepteeVersEnRoute() {

        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        // 1. Simuler l'ambulancier connecté
        connecterAmbulancier(ambulancierId);

        // 2. Créer une mission au statut ACCEPTEE
        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.ACCEPTEE);
        mission.setAmbulancierKeycloakId(ambulancierId);
        mission.setCommentaire("Mission de test");
        mission.setCreeLe(Instant.now());

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        when(missionRepository.save(any(Mission.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // 3. Simuler la synchronisation d'une transaction
        TransactionSynchronizationManager.initSynchronization();

        try {
            // 4. Changer le statut
            var resultat = service.changerStatut(
                missionId,
                new ChangerStatutMissionRequest(
                    StatutMission.EN_ROUTE
                )
            );

            // 5. Vérifier le nouveau statut
            assertEquals(
                StatutMission.EN_ROUTE,
                resultat.statut()
            );

            // 6. Vérifier l'enregistrement de la mission
            verify(missionRepository).save(any(Mission.class));

            // L'événement ne doit pas encore être publié
            verifyNoInteractions(eventPublisher);

            // 7. Simuler la confirmation de la transaction
            for (TransactionSynchronization synchronisation :
                TransactionSynchronizationManager.getSynchronizations()) {

                synchronisation.afterCommit();
            }

            // 8. Vérifier la publication de l'événement
            verify(eventPublisher).publierChangementStatut(
                missionId,
                "ACCEPTEE",
                "EN_ROUTE"
            );

            verify(eventPublisher, never())
                .publierMissionTerminee(any());

        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }


    @Test
    void terminerMissionPublieDeuxEvenements() {

        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        // 1. Simuler l'ambulancier connecté
        connecterAmbulancier(ambulancierId);

        // 2. Préparer une mission au statut SUR_PLACE
        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.SUR_PLACE);
        mission.setAmbulancierKeycloakId(ambulancierId);
        mission.setCommentaire("Intervention de test");
        mission.setCreeLe(Instant.now());

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        when(missionRepository.save(any(Mission.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // 3. Simuler une transaction
        TransactionSynchronizationManager.initSynchronization();

        try {

            // 4. Terminer la mission
            var resultat = service.changerStatut(
                missionId,
                new ChangerStatutMissionRequest(
                    StatutMission.TERMINEE
                )
            );

            // 5. Vérifier le statut final
            assertEquals(
                StatutMission.TERMINEE,
                resultat.statut()
            );

            // 6. Vérifier l'enregistrement
            verify(missionRepository)
                .save(any(Mission.class));

            // Aucun événement avant le commit
            verifyNoInteractions(eventPublisher);

            // 7. Simuler le commit de la transaction
            for (TransactionSynchronization synchronisation :
                TransactionSynchronizationManager.getSynchronizations()) {

                synchronisation.afterCommit();
            }

            // 8. Vérifier l'événement mission.statut
            verify(eventPublisher).publierChangementStatut(
                missionId,
                "SUR_PLACE",
                "TERMINEE"
            );

            // 9. Vérifier l'événement mission.terminee
            verify(eventPublisher).publierMissionTerminee(
                missionId
            );

        } finally {

            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void autreAmbulancierNePeutPasModifierStatutMission() {

        // 1. Préparer les identifiants
        UUID missionId = UUID.randomUUID();

        String ambulancierAffecte =
            UUID.randomUUID().toString();

        String autreAmbulancier =
            UUID.randomUUID().toString();

        // 2. Connecter un autre ambulancier
        connecterAmbulancier(autreAmbulancier);

        // 3. Préparer une mission qui ne lui appartient pas
        Mission mission = new Mission();

        mission.setId(missionId);
        mission.setStatut(StatutMission.ACCEPTEE);
        mission.setAmbulancierKeycloakId(ambulancierAffecte);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        // 4. Tenter une modification de statut
        ChangerStatutMissionRequest request =
            new ChangerStatutMissionRequest(
                StatutMission.EN_ROUTE
            );

        // 5. Vérifier que l'accès est refusé
        assertThrows(
            AccessDeniedException.class,
            () -> service.changerStatut(missionId, request)
        );

        // 6. Vérifier que le statut reste inchangé
        assertEquals(
            StatutMission.ACCEPTEE,
            mission.getStatut()
        );

        // 7. Vérifier qu'aucune modification n'a été enregistrée
        verify(missionRepository, never())
            .save(any(Mission.class));

        verifyNoInteractions(historiqueRepository);
        verifyNoInteractions(eventPublisher);
    }



}
