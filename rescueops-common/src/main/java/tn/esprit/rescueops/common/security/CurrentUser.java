package tn.esprit.rescueops.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.Set;

/**
 * Utilisateur courant, toujours dérivé du JWT validé (jamais d'un paramètre envoyé par le navigateur).
 *
 * @param keycloakId le {@code sub} Keycloak : identifiant utilisateur partagé entre tous les services
 */
public record CurrentUser(String keycloakId, String email, String givenName, String familyName, Set<Role> roles) {

    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    public static CurrentUser from(Jwt jwt) {
        return new CurrentUser(
                jwt.getSubject(),
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                KeycloakRoleConverter.rolesOf(jwt));
    }

    /** Utilisateur de la requête en cours, vide si la requête n'est pas authentifiée par JWT. */
    public static Optional<CurrentUser> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(from(jwt));
        }
        return Optional.empty();
    }

    public boolean has(Role role) {
        return roles.contains(role);
    }
}
