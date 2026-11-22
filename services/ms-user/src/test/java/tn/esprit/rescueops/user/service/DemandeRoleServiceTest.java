package tn.esprit.rescueops.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tn.esprit.rescueops.common.error.BadRequestException;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.user.domain.DemandeRole;
import tn.esprit.rescueops.user.domain.StatutDemandeRole;
import tn.esprit.rescueops.user.dto.DemandeRoleRequest;
import tn.esprit.rescueops.user.keycloak.KeycloakAdminClient;
import tn.esprit.rescueops.user.keycloak.KeycloakIndisponibleException;
import tn.esprit.rescueops.user.repository.DemandeRoleRepository;
import tn.esprit.rescueops.user.repository.UserProfileRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DemandeRoleServiceTest {

    private DemandeRoleRepository demandes;
    private KeycloakAdminClient keycloak;
    private DemandeRoleService service;

    private static final CurrentUser CITOYEN = new CurrentUser("sub-c", "c@x", "C", "C", Set.of(Role.CITOYEN));
    private static final CurrentUser ADMIN = new CurrentUser("sub-admin", "a@x", "A", "A", Set.of(Role.ADMIN));

    @BeforeEach
    void setUp() {
        demandes = mock(DemandeRoleRepository.class);
        keycloak = mock(KeycloakAdminClient.class);
        service = new DemandeRoleService(demandes, mock(UserProfileRepository.class), keycloak);
        when(demandes.save(any())).thenAnswer(i -> {
            DemandeRole d = i.getArgument(0);
            if (d.getId() == null) {
                d.setId(UUID.randomUUID());
            }
            return d;
        });
    }

    private DemandeRole enAttente(Role role) {
        DemandeRole d = new DemandeRole();
        d.setId(UUID.randomUUID());
        d.setKeycloakId("sub-c");
        d.setRoleDemande(role);
        d.setOrganisation("SAMU Tunis");
        d.setStatut(StatutDemandeRole.EN_ATTENTE);
        when(demandes.findById(d.getId())).thenReturn(Optional.of(d));
        return d;
    }

    @Test
    void demanderUnRoleProfessionnelCreeUneDemandeEnAttente() {
        var r = service.demander(CITOYEN, new DemandeRoleRequest(Role.AMBULANCIER, "SAMU Tunis", "Ambulancier depuis 2020"));

        ArgumentCaptor<DemandeRole> d = ArgumentCaptor.forClass(DemandeRole.class);
        verify(demandes).save(d.capture());
        assertThat(d.getValue().getKeycloakId()).isEqualTo("sub-c");
        assertThat(d.getValue().getStatut()).isEqualTo(StatutDemandeRole.EN_ATTENTE);
        assertThat(r.roleDemande()).isEqualTo(Role.AMBULANCIER);
    }

    @Test
    void demanderAdminEstInterdit() {
        assertThatThrownBy(() -> service.demander(CITOYEN, new DemandeRoleRequest(Role.ADMIN, "X", null)))
                .isInstanceOf(BadRequestException.class).extracting("code").isEqualTo("ROLE_NON_DEMANDABLE");
        verify(demandes, never()).save(any());
    }

    @Test
    void demanderCitoyenEstInutile() {
        assertThatThrownBy(() -> service.demander(CITOYEN, new DemandeRoleRequest(Role.CITOYEN, "X", null)))
                .isInstanceOf(BadRequestException.class).extracting("code").isEqualTo("ROLE_NON_DEMANDABLE");
    }

    @Test
    void uneSeuleDemandeEnAttenteParUtilisateur() {
        when(demandes.existsByKeycloakIdAndStatut("sub-c", StatutDemandeRole.EN_ATTENTE)).thenReturn(true);

        assertThatThrownBy(() -> service.demander(CITOYEN, new DemandeRoleRequest(Role.OPERATEUR, "X", null)))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("DEMANDE_EN_ATTENTE");
    }

    @Test
    void approuverAttribueLeRoleDansKeycloakPuisAccepte() {
        DemandeRole d = enAttente(Role.AMBULANCIER);
        when(demandes.cloturer(eq(d.getId()), eq(StatutDemandeRole.ACCEPTEE), eq("sub-admin"), isNull(), any())).thenReturn(1);

        service.approuver(d.getId(), ADMIN);

        var ordre = inOrder(keycloak, demandes);
        ordre.verify(keycloak).attribuerRole("sub-c", "AMBULANCIER");
        ordre.verify(demandes).cloturer(eq(d.getId()), eq(StatutDemandeRole.ACCEPTEE), eq("sub-admin"), isNull(), any());
    }

    @Test
    void keycloakIndisponibleLaisseLaDemandeEnAttente() {
        DemandeRole d = enAttente(Role.AMBULANCIER);
        doThrow(new KeycloakIndisponibleException(new RuntimeException("connexion refusée")))
                .when(keycloak).attribuerRole(any(), any());

        assertThatThrownBy(() -> service.approuver(d.getId(), ADMIN))
                .isInstanceOf(KeycloakIndisponibleException.class).extracting("code").isEqualTo("KEYCLOAK_INDISPONIBLE");
        verify(demandes, never()).cloturer(any(), any(), any(), any(), any());
    }

    @Test
    void approuverUneDemandeDejaTraitee409() {
        DemandeRole d = enAttente(Role.AMBULANCIER);
        d.setStatut(StatutDemandeRole.REFUSEE);

        assertThatThrownBy(() -> service.approuver(d.getId(), ADMIN))
                .isInstanceOf(ConflictException.class).extracting("code").isEqualTo("DEMANDE_DEJA_TRAITEE");
        verifyNoInteractions(keycloak);
    }

    @Test
    void refuserNeToucheJamaisAKeycloak() {
        DemandeRole d = enAttente(Role.COORDINATEUR_HOSPITALIER);
        when(demandes.cloturer(eq(d.getId()), eq(StatutDemandeRole.REFUSEE), eq("sub-admin"), eq("justificatif manquant"), any()))
                .thenReturn(1);

        service.refuser(d.getId(), "justificatif manquant", ADMIN);

        verifyNoInteractions(keycloak);
    }
}
