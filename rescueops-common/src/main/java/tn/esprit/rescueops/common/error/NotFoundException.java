package tn.esprit.rescueops.common.error;

import org.springframework.http.HttpStatus;

/** Ressource absente : 404. */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super("RESSOURCE_INTROUVABLE", message);
    }

    public NotFoundException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.NOT_FOUND;
    }
}
