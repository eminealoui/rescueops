package tn.esprit.rescueops.common.error;

import org.springframework.http.HttpStatus;

/** Règle métier non respectée par une requête bien formée : 422. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super("REGLE_METIER", message);
    }

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }

    @Override
    public HttpStatus status() {
        return HttpStatus.UNPROCESSABLE_CONTENT;
    }
}
