package tn.esprit.rescueops.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private static Jwt jwt(Map<String, Object> claims) {
        Jwt.Builder b = Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("6f1c2d3e-0000-4000-8000-000000000001")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300));
        claims.forEach(b::claim);
        return b.build();
    }

    @Test
    void rolesKeycloakDeviennentAutorites() {
        Jwt jwt = jwt(Map.of("realm_access", Map.of("roles",
                List.of("ADMIN", "COORDINATEUR_HOSPITALIER", "offline_access", "uma_authorization"))));

        var auth = new KeycloakRoleConverter().convert(jwt);

        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_COORDINATEUR_HOSPITALIER");
        assertThat(auth.getName()).isEqualTo(jwt.getSubject());
    }

    @Test
    void sansRealmAccessAucuneAutorite() {
        var auth = new KeycloakRoleConverter().convert(jwt(Map.of("email", "x@rescueops.test")));

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void realmAccessMalFormeNeLevePasDException() {
        var auth = new KeycloakRoleConverter().convert(jwt(Map.of("realm_access", "pas-une-map")));

        assertThat(auth.getAuthorities()).isEmpty();
    }

    @Test
    void roleAuthorityEstPrefixe() {
        assertThat(Role.GESTIONNAIRE_RESSOURCES.authority()).isEqualTo("ROLE_GESTIONNAIRE_RESSOURCES");
        assertThat(Role.values()).extracting(Enum::name).containsExactly(
                "CITOYEN", "OPERATEUR", "GESTIONNAIRE_RESSOURCES", "AMBULANCIER",
                "COORDINATEUR_HOSPITALIER", "FORMATEUR", "APPRENANT", "ADMIN");
    }

    @Test
    void currentUserLitLesClaims() {
        Jwt jwt = jwt(Map.of(
                "email", "coordinateur@rescueops.test",
                "given_name", "Amine",
                "family_name", "Ben Salah",
                "realm_access", Map.of("roles", List.of("COORDINATEUR_HOSPITALIER", "CITOYEN", "default-roles-rescueops"))));

        CurrentUser user = CurrentUser.from(jwt);

        assertThat(user.keycloakId()).isEqualTo("6f1c2d3e-0000-4000-8000-000000000001");
        assertThat(user.email()).isEqualTo("coordinateur@rescueops.test");
        assertThat(user.givenName()).isEqualTo("Amine");
        assertThat(user.familyName()).isEqualTo("Ben Salah");
        assertThat(user.roles()).containsExactlyInAnyOrder(Role.COORDINATEUR_HOSPITALIER, Role.CITOYEN);
        assertThat(user.has(Role.COORDINATEUR_HOSPITALIER)).isTrue();
        assertThat(user.has(Role.ADMIN)).isFalse();
    }
}
