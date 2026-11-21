package tn.esprit.rescueops.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

/**
 * Mêmes règles que {@code rescueops-common} : l'émetteur est toujours l'URL publique de Keycloak,
 * les clés publiques sont lues sur une URL qui dépend du profil (local ou docker).
 */
@ConfigurationProperties("rescueops.security")
public record GatewaySecurityProperties(
        @DefaultValue("http://localhost:8180/realms/rescueops") String issuer,
        @DefaultValue("http://localhost:8180/realms/rescueops/protocol/openid-connect/certs") String jwkSetUri,
        @DefaultValue("http://localhost:4200") List<String> allowedOrigins) {
}
