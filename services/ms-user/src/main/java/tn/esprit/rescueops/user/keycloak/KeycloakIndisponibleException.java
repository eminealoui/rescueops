package tn.esprit.rescueops.user.keycloak;

import org.springframework.http.HttpStatus;
import tn.esprit.rescueops.common.error.ApiException;

/** Keycloak injoignable ou opération refusée : 503, rien n'a été modifié côté RescueOps. */
public class KeycloakIndisponibleException extends ApiException {

    public KeycloakIndisponibleException(Throwable cause) {
        super("KEYCLOAK_INDISPONIBLE", "Le service d'identité est momentanément indisponible. Réessayez dans un instant.");
        initCause(cause);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.SERVICE_UNAVAILABLE;
    }
}
