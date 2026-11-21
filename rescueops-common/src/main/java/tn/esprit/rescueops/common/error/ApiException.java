package tn.esprit.rescueops.common.error;

import org.springframework.http.HttpStatus;

/**
 * Exception métier traduite automatiquement en réponse ProblemDetail par {@link GlobalExceptionHandler}.
 * Le {@code code} est stable et lisible par le frontend (ex. {@code CAPACITE_EPUISEE}).
 */
public abstract class ApiException extends RuntimeException {

    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public abstract HttpStatus status();

    public String code() {
        return code;
    }
}
