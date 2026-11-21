package tn.esprit.rescueops.common.error;

import org.springframework.http.HttpStatus;

/** Conflit avec l'état actuel (doublon, place déjà prise, transition interdite) : 409. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super("CONFLIT", message);
    }

    public ConflictException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.CONFLICT;
    }
}
