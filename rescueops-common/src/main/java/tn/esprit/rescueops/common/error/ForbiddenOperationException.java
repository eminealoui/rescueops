package tn.esprit.rescueops.common.error;

import org.springframework.http.HttpStatus;

/** Opération interdite pour cet utilisateur malgré un rôle valide (ex. modifier le profil d'un autre) : 403. */
public class ForbiddenOperationException extends ApiException {

    public ForbiddenOperationException(String message) {
        super("OPERATION_INTERDITE", message);
    }

    public ForbiddenOperationException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.FORBIDDEN;
    }
}
