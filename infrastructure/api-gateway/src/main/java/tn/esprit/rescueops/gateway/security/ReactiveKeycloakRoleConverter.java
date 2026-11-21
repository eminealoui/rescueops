package tn.esprit.rescueops.gateway.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Version réactive du convertisseur de {@code rescueops-common} : mêmes 8 rôles, mêmes autorités ROLE_*. */
public class ReactiveKeycloakRoleConverter implements Converter<Jwt, Mono<AbstractAuthenticationToken>> {

    static final Set<String> ROLES = Set.of("CITOYEN", "OPERATEUR", "GESTIONNAIRE_RESSOURCES", "AMBULANCIER",
            "COORDINATEUR_HOSPITALIER", "FORMATEUR", "APPRENANT", "ADMIN");

    @Override
    public Mono<AbstractAuthenticationToken> convert(Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = List.of();
        if (jwt.getClaims().get("realm_access") instanceof Map<?, ?> realmAccess
                && realmAccess.get("roles") instanceof Collection<?> raw) {
            authorities = raw.stream()
                    .filter(r -> r instanceof String s && ROLES.contains(s))
                    .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                    .toList();
        }
        return Mono.just(new JwtAuthenticationToken(jwt, authorities, jwt.getSubject()));
    }
}
