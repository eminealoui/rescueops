
package tn.esprit.rescueops.intervention.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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


}
