package tn.esprit.rescueops.common.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Configuration de la validation JWT.
 *
 * @param issuer      émetteur attendu ; toujours l'URL publique de Keycloak, même dans Docker
 * @param jwkSetUri   URL des clés publiques ; {@code localhost:8180} en local, {@code keycloak:8080} dans Docker
 * @param publicPaths chemins accessibles sans token
 */
@ConfigurationProperties("rescueops.security")
public record RescueOpsSecurityProperties(
        @DefaultValue("http://localhost:8180/realms/rescueops") String issuer,
        @DefaultValue("http://localhost:8180/realms/rescueops/protocol/openid-connect/certs") String jwkSetUri,
        @DefaultValue({"/actuator/health/**", "/actuator/info", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"})
        List<String> publicPaths) {
}
