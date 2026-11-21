package tn.esprit.rescueops.user.web;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rescueops.common.error.BadRequestException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.user.dto.*;
import tn.esprit.rescueops.user.service.UserProfileService;

import java.util.Set;

/**
 * API des profils. Pour /me, l'identité vient du TOKEN : le navigateur ne peut pas choisir le profil visé.
 */
@RestController
@RequestMapping("/api/users")
public class UserProfileController {

    static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE = Set.of("lastName", "firstName", "email", "createdAt");

    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public UserProfileResponse me(JwtAuthenticationToken auth) {
        return service.getOrCreateCurrent(CurrentUser.from(auth.getToken()));
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(JwtAuthenticationToken auth, @Valid @RequestBody UpdateMyProfileRequest request) {
        return service.updateCurrent(CurrentUser.from(auth.getToken()), request);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<UserProfileResponse> search(@RequestParam(required = false) String query,
                                                    @RequestParam(required = false) Boolean active,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @RequestParam(defaultValue = "lastName,asc") String sort) {
        return service.search(query, active, Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), validSort(sort));
    }

    @GetMapping("/{keycloakId}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse byKeycloakId(@PathVariable String keycloakId) {
        return service.findByKeycloakId(keycloakId);
    }

    @PatchMapping("/{keycloakId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse setStatus(@PathVariable String keycloakId, @Valid @RequestBody UpdateStatusRequest request) {
        return service.setActive(keycloakId, request.active());
    }

    /** Nom affichable d'un utilisateur : accessible à tout utilisateur authentifié et aux autres services. */
    @GetMapping("/{keycloakId}/summary")
    public UserSummaryDto summary(@PathVariable String keycloakId) {
        return service.summary(keycloakId);
    }

    /** « champ,direction » avec un champ autorisé ; tout autre tri est refusé (400) plutôt que de planter. */
    private static String validSort(String sort) {
        String[] parts = sort.split(",");
        String field = parts[0].trim();
        String direction = parts.length > 1 ? parts[1].trim().toLowerCase() : "asc";
        if (!SORTABLE.contains(field) || !(direction.equals("asc") || direction.equals("desc"))) {
            throw new BadRequestException("TRI_INVALIDE",
                    "Tri non autorisé : « " + sort + " ». Champs possibles : " + String.join(", ", SORTABLE.stream().sorted().toList()) + ".");
        }
        return field + "," + direction;
    }
}
