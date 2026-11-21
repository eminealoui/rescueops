package tn.esprit.rescueops.common.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

/**
 * Règles de sécurité RescueOps réutilisables.
 * <p>
 * Point d'extension officiel pour un service qui a besoin de SA propre chaîne de sécurité :
 * <pre>{@code
 * @Bean
 * SecurityFilterChain maChaine(HttpSecurity http, RescueOpsSecurityProperties props) throws Exception {
 *     http.authorizeHttpRequests(a -> a.requestMatchers("/api/hospitals/public/**").permitAll());
 *     return RescueOpsSecurity.applyDefaults(http, props).build();
 * }
 * }</pre>
 * Les règles déclarées AVANT l'appel sont prioritaires. Sans cela, déclarer sa propre chaîne ferait perdre
 * la conversion des rôles Keycloak (tous les {@code hasRole} renverraient 403) et le format ProblemDetail.
 * Pour seulement ajouter des chemins publics, préférer la propriété {@code rescueops.security.public-paths}.
 */
public final class RescueOpsSecurity {

    private RescueOpsSecurity() {
    }

    public static HttpSecurity applyDefaults(HttpSecurity http, RescueOpsSecurityProperties props) throws Exception {
        ProblemDetailSecurityHandlers handlers = new ProblemDetailSecurityHandlers();
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(props.publicPaths().toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new KeycloakRoleConverter()))
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(handlers)
                        .accessDeniedHandler(handlers));
    }
}
