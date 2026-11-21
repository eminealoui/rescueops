package tn.esprit.rescueops.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.user.domain.UserProfile;
import tn.esprit.rescueops.user.dto.UserProfileResponse;
import tn.esprit.rescueops.user.mapper.UserProfileMapper;
import tn.esprit.rescueops.user.mapper.UserProfileMapperImpl;
import tn.esprit.rescueops.user.repository.UserProfileRepository;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import tn.esprit.rescueops.common.error.BadRequestException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserProfileServiceTest {

    private UserProfileRepository repository;
    private UserProfileService service;

    private static CurrentUser user(String email, String given, String family) {
        return new CurrentUser("sub-1", email, given, family, Set.of(Role.CITOYEN));
    }

    private static UserProfile existing(String email) {
        UserProfile p = new UserProfile();
        p.setKeycloakId("sub-1");
        p.setEmail(email);
        p.setFirstName("Amine");
        p.setActive(true);
        return p;
    }

    @BeforeEach
    void setUp() {
        repository = mock(UserProfileRepository.class);
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        UserProfileMapper mapper = new UserProfileMapperImpl();
        service = new UserProfileService(repository, mapper, tx);
        when(repository.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void premierAccesCreeLeProfilDepuisLeToken() {
        when(repository.findByKeycloakId("sub-1")).thenReturn(Optional.empty());

        UserProfileResponse r = service.getOrCreateCurrent(user("a@rescueops.test", "Amine", "Ben Salah"));

        ArgumentCaptor<UserProfile> saved = ArgumentCaptor.forClass(UserProfile.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getKeycloakId()).isEqualTo("sub-1");
        assertThat(saved.getValue().getEmail()).isEqualTo("a@rescueops.test");
        assertThat(saved.getValue().getFirstName()).isEqualTo("Amine");
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(r.keycloakId()).isEqualTo("sub-1");
        assertThat(r.lastName()).isEqualTo("Ben Salah");
    }

    @Test
    void secondAccesRenvoieLeProfilSansRecreer() {
        when(repository.findByKeycloakId("sub-1")).thenReturn(Optional.of(existing("a@rescueops.test")));

        UserProfileResponse r = service.getOrCreateCurrent(user("a@rescueops.test", "Amine", null));

        verify(repository, never()).save(any());
        assertThat(r.firstName()).isEqualTo("Amine");
    }

    @Test
    void emailChangeDansKeycloakEstResynchronise() {
        UserProfile p = existing("old@rescueops.test");
        when(repository.findByKeycloakId("sub-1")).thenReturn(Optional.of(p));

        UserProfileResponse r = service.getOrCreateCurrent(user("new@rescueops.test", "Amine", null));

        assertThat(r.email()).isEqualTo("new@rescueops.test");
        assertThat(p.getEmail()).isEqualTo("new@rescueops.test");
    }

    @Test
    void tokenSansNomCreeUnProfilSansErreur() {
        when(repository.findByKeycloakId("sub-1")).thenReturn(Optional.empty());

        UserProfileResponse r = service.getOrCreateCurrent(user("a@rescueops.test", null, null));

        assertThat(r.firstName()).isNull();
        assertThat(r.lastName()).isNull();
        verify(repository).save(any());
    }

    /** Revue I2 : le résumé public ne doit jamais exposer l'email, même si le profil n'a pas de nom. */
    @Test
    void resumeSansNomNExposePasLEmail() {
        UserProfile p = existing("secret@rescueops.test");
        p.setFirstName(null);
        when(repository.findByKeycloakId("sub-1")).thenReturn(Optional.of(p));

        assertThat(service.summary("sub-1").displayName()).isEqualTo("Utilisateur");
    }

    /** Revue I3 : un token sans « sub » est refusé proprement (400), pas par une erreur SQL. */
    @Test
    void tokenSansSujetEstRefuse() {
        CurrentUser sansSub = new CurrentUser(null, "a@rescueops.test", "A", "B", Set.of(Role.CITOYEN));

        assertThatThrownBy(() -> service.getOrCreateCurrent(sansSub))
                .isInstanceOf(BadRequestException.class)
                .extracting("code").isEqualTo("TOKEN_SANS_SUJET");
        verify(repository, never()).save(any());
    }

    @Test
    void courseDeCreationRelitLeProfilExistant() {
        when(repository.findByKeycloakId("sub-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing("a@rescueops.test")));
        when(repository.save(any(UserProfile.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry 'sub-1'"));

        UserProfileResponse r = service.getOrCreateCurrent(user("a@rescueops.test", "Amine", null));

        assertThat(r.keycloakId()).isEqualTo("sub-1");
    }
}
