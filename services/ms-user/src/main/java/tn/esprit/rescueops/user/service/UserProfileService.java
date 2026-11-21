package tn.esprit.rescueops.user.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import tn.esprit.rescueops.common.error.ForbiddenOperationException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.user.dto.PageResponse;
import tn.esprit.rescueops.user.dto.UpdateMyProfileRequest;
import tn.esprit.rescueops.user.dto.UserSummaryDto;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.user.domain.UserProfile;
import tn.esprit.rescueops.user.dto.UserProfileResponse;
import tn.esprit.rescueops.user.mapper.UserProfileMapper;
import tn.esprit.rescueops.user.repository.UserProfileRepository;

import java.util.Objects;
import java.util.Optional;

/**
 * Règles métier des profils. L'identité vient TOUJOURS du token (CurrentUser), jamais du navigateur.
 */
@Service
public class UserProfileService {

    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;
    private final TransactionTemplate tx;
    private final TransactionTemplate newTx;

    public UserProfileService(UserProfileRepository repository, UserProfileMapper mapper,
                              PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.mapper = mapper;
        this.tx = new TransactionTemplate(transactionManager);
        this.newTx = new TransactionTemplate(transactionManager);
        this.newTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Profil de l'utilisateur connecté, créé au premier accès à partir du JWT.
     * Deux premiers accès simultanés : l'un crée, l'autre bute sur la contrainte unique et relit le profil créé.
     */
    public UserProfileResponse getOrCreateCurrent(CurrentUser user) {
        Optional<UserProfile> found = tx.execute(s -> repository.findByKeycloakId(user.keycloakId())
                .map(p -> syncEmail(p, user)));
        if (found.isPresent()) {
            return mapper.toResponse(found.get());
        }
        try {
            return mapper.toResponse(newTx.execute(s -> repository.save(newProfile(user))));
        } catch (DataIntegrityViolationException raceOrDuplicate) {
            // Course entre deux requêtes : le profil vient d'être créé par l'autre. Sinon (email déjà pris) → 409.
            return tx.execute(s -> repository.findByKeycloakId(user.keycloakId()))
                    .map(mapper::toResponse)
                    .orElseThrow(() -> raceOrDuplicate);
        }
    }

    /** Modifie les champs personnels du profil connecté. Un profil désactivé ne peut plus être modifié. */
    public UserProfileResponse updateCurrent(CurrentUser user, UpdateMyProfileRequest request) {
        getOrCreateCurrent(user);
        return tx.execute(s -> {
            UserProfile p = repository.findByKeycloakId(user.keycloakId()).orElseThrow();
            if (!p.isActive()) {
                throw new ForbiddenOperationException("Profil désactivé : modification impossible.");
            }
            p.setFirstName(request.firstName().trim());
            p.setLastName(request.lastName().trim());
            p.setPhone(blankToNull(request.phone()));
            p.setAddress(blankToNull(request.address()));
            return mapper.toResponse(repository.save(p));
        });
    }

    public UserProfileResponse findByKeycloakId(String keycloakId) {
        return tx.execute(s -> mapper.toResponse(load(keycloakId)));
    }

    public UserSummaryDto summary(String keycloakId) {
        return tx.execute(s -> {
            UserProfile p = load(keycloakId);
            String name = String.join(" ", Stream.of(p.getFirstName(), p.getLastName())
                    .filter(v -> v != null && !v.isBlank()).toList());
            return new UserSummaryDto(p.getKeycloakId(), name.isBlank() ? p.getEmail() : name, p.getOrganisation());
        });
    }

    /** Recherche paginée. {@code sort} est déjà validé par le contrôleur (champ autorisé + direction). */
    public PageResponse<UserProfileResponse> search(String query, Boolean active, int page, int size, String sort) {
        String[] parts = sort.split(",");
        Sort order = Sort.by(Sort.Direction.fromString(parts[1]), parts[0]);
        String q = query == null || query.isBlank() ? null : query.trim();
        return tx.execute(s -> PageResponse.of(
                repository.search(q, active, PageRequest.of(page, size, order)), mapper::toResponse));
    }

    public UserProfileResponse setActive(String keycloakId, boolean active) {
        return tx.execute(s -> {
            UserProfile p = load(keycloakId);
            p.setActive(active);
            return mapper.toResponse(repository.save(p));
        });
    }

    private UserProfile load(String keycloakId) {
        return repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new NotFoundException("Profil introuvable."));
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    /** Keycloak fait autorité sur l'email : on le resynchronise s'il a changé. */
    private UserProfile syncEmail(UserProfile profile, CurrentUser user) {
        if (user.email() != null && !Objects.equals(profile.getEmail(), user.email())) {
            profile.setEmail(user.email());
            return repository.save(profile);
        }
        return profile;
    }

    private static UserProfile newProfile(CurrentUser user) {
        UserProfile p = new UserProfile();
        p.setKeycloakId(user.keycloakId());
        p.setEmail(user.email());
        p.setFirstName(user.givenName());
        p.setLastName(user.familyName());
        p.setActive(true);
        return p;
    }
}
