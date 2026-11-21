package tn.esprit.rescueops.resource.web;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Endpoint de vérification : prouve que la chaîne complète fonctionne
 * (Gateway → Eureka → ce service → sécurité JWT). À garder tel quel.
 */
@RestController
public class PingController {

    @GetMapping("/api/resources/ping")
    Map<String, Object> ping(JwtAuthenticationToken auth) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "ms-resource");
        body.put("statut", "UP");
        body.put("utilisateur", auth.getToken().getSubject());
        body.put("roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList());
        return body;
    }
}
