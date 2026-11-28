package tn.esprit.rescueops.gateway.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import tn.esprit.rescueops.gateway.error.Problems;

import java.time.Duration;
import java.util.List;

/**
 * Première barrière : tout appel /api/** exige un JWT Keycloak valide (signature, dates, émetteur).
 * Les microservices revalident le token : la Gateway n'est pas la seule frontière de sécurité.
 */
@Configuration
@EnableWebFluxSecurity
public class GatewaySecurityConfig {

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(GatewaySecurityProperties props) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder.withJwkSetUri(props.jwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(props.issuer()));
        return decoder;
    }

    @Bean
    SecurityWebFilterChain gatewaySecurity(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> {
                })
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/actuator/health/**", "/actuator/health", "/actuator/info", "/actuator/prometheus",
                                "/docs/*/v3/api-docs", "/docs/*/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                        .permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new ReactiveKeycloakRoleConverter()))
                        .authenticationEntryPoint((exchange, ex) -> Problems.write(exchange, HttpStatus.UNAUTHORIZED,
                                "NON_AUTHENTIFIE", "Authentification requise : jeton absent, expiré ou invalide."))
                        .accessDeniedHandler((exchange, ex) -> Problems.write(exchange, HttpStatus.FORBIDDEN,
                                "ACCES_REFUSE", "Vos droits ne permettent pas cette opération.")))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((exchange, ex) -> Problems.write(exchange, HttpStatus.UNAUTHORIZED,
                                "NON_AUTHENTIFIE", "Authentification requise : jeton absent, expiré ou invalide.")))
                .build();
    }

    /** CORS restreint au frontend Angular (liste configurable). */
    @Bean
    CorsConfigurationSource corsConfigurationSource(GatewaySecurityProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-Id"));
        config.setExposedHeaders(List.of("X-Correlation-Id", "Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
