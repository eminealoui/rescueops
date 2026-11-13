package tn.esprit.rescueops.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/** Le gateway vérifie le JWT Keycloak ; les rôles fins sont contrôlés dans chaque service. */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    SecurityWebFilterChain chain(ServerHttpSecurity http) {
        return http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(e -> e
        .pathMatchers("/actuator/health").permitAll()
        .pathMatchers("/api/emergency/public/**").permitAll()
        .anyExchange().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> {}))
        .build();
    }
}
