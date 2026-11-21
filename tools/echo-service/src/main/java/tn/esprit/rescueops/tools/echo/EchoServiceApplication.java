package tn.esprit.rescueops.tools.echo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.TreeMap;

/** Faux « ms-hospital » : renvoie ce que le service reçoit réellement (identité JWT et en-têtes). */
@SpringBootApplication
@RestController
public class EchoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EchoServiceApplication.class, args);
    }

    @GetMapping("/api/hospitals/echo")
    Map<String, Object> echo(JwtAuthenticationToken auth, @RequestHeader HttpHeaders headers) {
        Map<String, String> received = new TreeMap<>();
        headers.forEach((name, values) -> {
            if (!name.equalsIgnoreCase(HttpHeaders.AUTHORIZATION)) {
                received.put(name.toLowerCase(), String.join(",", values));
            }
        });
        return Map.of(
                "sub", auth.getToken().getSubject(),
                "roles", auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList(),
                "headers", received);
    }
}
