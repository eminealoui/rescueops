package tn.esprit.rescueops.user.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Client de l'API d'administration Keycloak, authentifié par le compte de service du client confidentiel
 * « ms-user-admin » (rôles realm-management : manage-users, view-realm). Le secret vient de .env, jamais du code.
 */
@Component
@EnableConfigurationProperties(RestKeycloakAdminClient.Proprietes.class)
public class RestKeycloakAdminClient implements KeycloakAdminClient {

    @ConfigurationProperties("rescueops.keycloak.admin")
    public record Proprietes(String baseUrl, String realm, String clientId, String clientSecret) {
    }

    private final Proprietes props;
    private final RestClient http;

    public RestKeycloakAdminClient(Proprietes props, RestClient.Builder builder) {
        this.props = props;
        this.http = builder.baseUrl(props.baseUrl()).build();
    }

    @Override
    public void attribuerRole(String keycloakId, String role) {
        try {
            String token = jetonDeService();
            Map<?, ?> representation = http.get()
                    .uri("/admin/realms/{realm}/roles/{role}", props.realm(), role)
                    .headers(h -> h.setBearerAuth(token))
                    .retrieve().body(Map.class);
            http.post()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", props.realm(), keycloakId)
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(representation))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            throw new KeycloakIndisponibleException(e);
        }
    }

    private String jetonDeService() {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        Map<?, ?> reponse = http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", props.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve().body(Map.class);
        return String.valueOf(reponse.get("access_token"));
    }
}
