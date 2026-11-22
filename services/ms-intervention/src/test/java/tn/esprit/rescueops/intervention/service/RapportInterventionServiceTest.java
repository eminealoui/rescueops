
package tn.esprit.rescueops.intervention.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.security.access.AccessDeniedException;

import tn.esprit.rescueops.intervention.domain.RapportIntervention;
import org.springframework.security.access.AccessDeniedException;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tn.esprit.rescueops.intervention.domain.RapportIntervention;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.intervention.domain.Mission;
import tn.esprit.rescueops.intervention.domain.StatutMission;
import tn.esprit.rescueops.intervention.dto.RapportRequest;
import tn.esprit.rescueops.intervention.repository.MissionRepository;
import tn.esprit.rescueops.intervention.repository.RapportInterventionRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
@ExtendWith(MockitoExtension.class)
class RapportInterventionServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private RapportInterventionRepository rapportRepository;

    @InjectMocks
    private RapportInterventionService service;

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

        JwtAuthenticationToken auth = new JwtAuthenticationToken(
            jwt,
            List.of(new SimpleGrantedAuthority(
                "ROLE_AMBULANCIER"
            ))
        );

        SecurityContextHolder.getContext()
            .setAuthentication(auth);
    }

    @Test
    void refuserRapportSiMissionNonTerminee() {

        // 1. Préparer les identifiants
        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        // 2. Simuler l'ambulancier connecté
        connecterAmbulancier(ambulancierId);

        // 3. Préparer une mission encore EN_ROUTE
        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.EN_ROUTE);
        mission.setAmbulancierKeycloakId(ambulancierId);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        // 4. Préparer une demande de rapport
        RapportRequest request = new RapportRequest(
            "Bilan de test",
            "Premiers secours",
            "Observations de test"
        );

        // 5. Vérifier que la création est refusée
        assertThrows(
            ConflictException.class,
            () -> service.creer(missionId, request)
        );

        // 6. Vérifier qu'aucun rapport n'est enregistré
        verify(rapportRepository, never())
            .save(any());
    }

    @Test
    void refuserDeuxiemeRapportPourMemeMission() {

        // 1. Préparer les identifiants
        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        // 2. Simuler l'ambulancier connecté
        connecterAmbulancier(ambulancierId);

        // 3. Préparer une mission TERMINEE
        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.TERMINEE);
        mission.setAmbulancierKeycloakId(ambulancierId);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        // 4. Simuler un rapport qui existe déjà
        when(rapportRepository.existsByMission_Id(missionId))
            .thenReturn(true);

        // 5. Préparer une deuxième demande de rapport
        RapportRequest request = new RapportRequest(
            "Deuxieme bilan",
            "Actions de test",
            "Ce rapport doit etre refuse"
        );

        // 6. Vérifier que l'application refuse la duplication
        assertThrows(
            ConflictException.class,
            () -> service.creer(missionId, request)
        );

        // 7. Vérifier qu'aucun nouveau rapport n'est enregistré
        verify(rapportRepository, never())
            .save(any());
    }

    @Test
    void creerRapportSiMissionTerminee() {

        // 1. Préparer les identifiants
        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        // 2. Simuler l'ambulancier connecté
        connecterAmbulancier(ambulancierId);

        // 3. Préparer une mission terminée
        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.TERMINEE);
        mission.setAmbulancierKeycloakId(ambulancierId);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        // Aucun rapport n'existe encore :
        // Mockito renvoie false par défaut pour existsByMission_Id()

        // 4. Simuler l'enregistrement en base de données
        when(rapportRepository.save(any(RapportIntervention.class)))
            .thenAnswer(invocation -> {

                RapportIntervention rapport =
                    invocation.getArgument(0);

                rapport.setId(UUID.randomUUID());

                return rapport;
            });

        // 5. Préparer la demande de rapport
        RapportRequest request = new RapportRequest(
            "Intervention terminee avec succes",
            "Premiers secours realises",
            "Patient pris en charge"
        );

        // 6. Créer le rapport
        var resultat = service.creer(missionId, request);

        // 7. Vérifier le résultat
        assertNotNull(resultat);

        assertEquals(missionId, resultat.missionId());

        assertEquals(
            "Intervention terminee avec succes",
            resultat.bilan()
        );

        assertEquals(
            ambulancierId,
            resultat.creePar()
        );

        // 8. Vérifier qu'un seul rapport est enregistré
        verify(rapportRepository, times(1))
            .save(any(RapportIntervention.class));
    }

    @Test
    void autreAmbulancierNePeutPasCreerRapport() {

        // 1. Préparer les identifiants
        UUID missionId = UUID.randomUUID();

        String ambulancierAffecte =
            UUID.randomUUID().toString();

        String autreAmbulancier =
            UUID.randomUUID().toString();

        // 2. Simuler la connexion d'un autre ambulancier
        connecterAmbulancier(autreAmbulancier);

        // 3. Préparer une mission terminée
        Mission mission = new Mission();

        mission.setId(missionId);
        mission.setStatut(StatutMission.TERMINEE);
        mission.setAmbulancierKeycloakId(ambulancierAffecte);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        // 4. Préparer une demande de rapport
        RapportRequest request = new RapportRequest(
            "Bilan non autorise",
            "Actions de test",
            "Ce rapport doit etre refuse"
        );

        // 5. Vérifier que l'accès est interdit
        assertThrows(
            AccessDeniedException.class,
            () -> service.creer(missionId, request)
        );

        // 6. Vérifier qu'aucun rapport n'a été enregistré
        verify(rapportRepository, never())
            .save(any());
    }

    @Test
    void modifierRapportExistant() {

        UUID missionId = UUID.randomUUID();
        UUID rapportId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        connecterAmbulancier(ambulancierId);

        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.TERMINEE);
        mission.setAmbulancierKeycloakId(ambulancierId);

        RapportIntervention rapport = new RapportIntervention();
        rapport.setId(rapportId);
        rapport.setMission(mission);
        rapport.setBilan("Ancien bilan");
        rapport.setCreePar(ambulancierId);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        when(rapportRepository.findByMission_Id(missionId))
            .thenReturn(Optional.of(rapport));

        when(rapportRepository.save(any(RapportIntervention.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        RapportRequest request = new RapportRequest(
            "Nouveau bilan",
            "Nouvelles actions",
            "Nouvelles observations"
        );

        var resultat = service.modifier(missionId, request);

        assertEquals(rapportId, resultat.id());
        assertEquals(missionId, resultat.missionId());
        assertEquals("Nouveau bilan", resultat.bilan());
        assertEquals("Nouvelles actions", resultat.actionsRealisees());
        assertEquals("Nouvelles observations", resultat.observations());
        assertEquals(ambulancierId, resultat.creePar());

        verify(rapportRepository).save(rapport);
    }

    @Test
    void supprimerRapportExistant() {

        UUID missionId = UUID.randomUUID();
        String ambulancierId = UUID.randomUUID().toString();

        connecterAmbulancier(ambulancierId);

        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.TERMINEE);
        mission.setAmbulancierKeycloakId(ambulancierId);

        RapportIntervention rapport = new RapportIntervention();
        rapport.setId(UUID.randomUUID());
        rapport.setMission(mission);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        when(rapportRepository.findByMission_Id(missionId))
            .thenReturn(Optional.of(rapport));

        service.supprimer(missionId);

        verify(rapportRepository, times(1))
            .delete(rapport);

        verify(missionRepository, never())
            .delete(any(Mission.class));
    }

    @Test
    void autreAmbulancierNePeutPasSupprimerRapport() {

        UUID missionId = UUID.randomUUID();

        String ambulancierAffecte = UUID.randomUUID().toString();
        String autreAmbulancier = UUID.randomUUID().toString();

        connecterAmbulancier(autreAmbulancier);

        Mission mission = new Mission();
        mission.setId(missionId);
        mission.setStatut(StatutMission.TERMINEE);
        mission.setAmbulancierKeycloakId(ambulancierAffecte);

        when(missionRepository.findById(missionId))
            .thenReturn(Optional.of(mission));

        assertThrows(
            AccessDeniedException.class,
            () -> service.supprimer(missionId)
        );

        verify(rapportRepository, never())
            .delete(any(RapportIntervention.class));
    }

    @Test
    void listerRapportsUtilisateurConnecte() {

        String ambulancierId = UUID.randomUUID().toString();

        connecterAmbulancier(ambulancierId);

        Pageable pageable = PageRequest.of(0, 10);

        Mission mission = new Mission();
        mission.setId(UUID.randomUUID());
        mission.setStatut(StatutMission.TERMINEE);
        mission.setAmbulancierKeycloakId(ambulancierId);

        RapportIntervention rapport = new RapportIntervention();

        rapport.setId(UUID.randomUUID());
        rapport.setMission(mission);
        rapport.setBilan("Intervention terminee");
        rapport.setCreePar(ambulancierId);
        rapport.setCreeLe(Instant.now());

        when(rapportRepository.findPourUtilisateur(
            ambulancierId, pageable))
            .thenReturn(new PageImpl<>(
                List.of(rapport), pageable, 1
            ));

        var resultat = service.lister(pageable);

        assertEquals(1, resultat.getTotalElements());

        assertEquals(
            rapport.getId(),
            resultat.getContent().get(0).id()
        );

        verify(rapportRepository)
            .findPourUtilisateur(ambulancierId, pageable);

        verify(rapportRepository, never())
            .findAll(pageable);
    }






}
