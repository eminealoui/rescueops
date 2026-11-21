package tn.esprit.rescueops.common.error;

import org.springframework.http.HttpStatus;

/** Paramètre de requête invalide (tri inconnu, filtre incohérent…) : 400. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super("REQUETE_INVALIDE", message);
    }

    public BadRequestException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.BAD_REQUEST;
    }
}
