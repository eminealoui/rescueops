package tn.esprit.rescueops.common.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Convertit un JWT Keycloak en authentification Spring.
 * Lit {@code realm_access.roles} et ne garde que les rôles RescueOps connus
 * (les rôles techniques Keycloak comme {@code offline_access} sont ignorés).
 */
public class KeycloakRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = rolesOf(jwt).stream()
                .map(r -> new SimpleGrantedAuthority(r.authority()))
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    /** Rôles RescueOps présents dans {@code realm_access.roles}, ensemble vide si la claim est absente ou mal formée. */
    static Set<Role> rolesOf(Jwt jwt) {
        Set<Role> roles = new LinkedHashSet<>();
        if (jwt.getClaims().get("realm_access") instanceof Map<?, ?> realmAccess
                && realmAccess.get("roles") instanceof Collection<?> raw) {
            for (Object value : raw) {
                Role role = value instanceof String s ? Role.fromName(s) : null;
                if (role != null) {
                    roles.add(role);
                }
            }
        }
        return roles;
    }
}
