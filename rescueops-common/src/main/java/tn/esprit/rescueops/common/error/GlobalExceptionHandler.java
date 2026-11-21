package tn.esprit.rescueops.common.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Map;

/**
 * Traduction centralisée des exceptions en RFC 9457 ProblemDetail.
 * Règle : jamais de détail technique (SQL, pile, message interne) dans la réponse ; tout va dans les logs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ProblemDetail api(ApiException ex, HttpServletRequest req) {
        return ProblemResponses.of(ex.status(), ex.code(), ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(), "message", String.valueOf(fe.getDefaultMessage())))
                .toList();
        return validation(errors, req);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail invalidParams(ConstraintViolationException ex, HttpServletRequest req) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(v -> Map.of("field", lastNode(v.getPropertyPath().toString()), "message", v.getMessage()))
                .toList();
        return validation(errors, req);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail unreadable(Exception ex, HttpServletRequest req) {
        return ProblemResponses.of(HttpStatus.BAD_REQUEST, "REQUETE_ILLISIBLE",
                "Le corps ou un paramètre de la requête est mal formé.", req.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violation de contrainte en base sur {} : {}", req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ProblemResponses.of(HttpStatus.CONFLICT, "CONFLIT_DONNEES",
                "La donnée entre en conflit avec une donnée existante.", req.getRequestURI());
    }

    /** Deux modifications simultanées du même enregistrement (@Version) : le client doit recharger et réessayer. */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ProblemDetail concurrentModification(OptimisticLockingFailureException ex, HttpServletRequest req) {
        log.info("Modification concurrente sur {} : {}", req.getRequestURI(), ex.getMessage());
        return ProblemResponses.of(HttpStatus.CONFLICT, "MODIFICATION_CONCURRENTE",
                "Cette donnée vient d'être modifiée par ailleurs. Rechargez-la puis réessayez.", req.getRequestURI());
    }

    /** {@code @PreAuthorize} refusé : même réponse que le filtre de sécurité. */
    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    ProblemDetail denied(Exception ex, HttpServletRequest req) {
        return ProblemResponses.of(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                "Vos droits ne permettent pas cette opération.", req.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex, HttpServletRequest req) {
        // Erreurs standard de Spring MVC (404 route inconnue, 405 méthode, 415 média…) : on garde leur statut.
        if (ex instanceof ErrorResponse er && er.getStatusCode().value() < 500) {
            HttpStatus status = HttpStatus.valueOf(er.getStatusCode().value());
            return ProblemResponses.of(status, status.name(), er.getBody().getDetail(), req.getRequestURI());
        }
        log.error("Erreur inattendue sur {}", req.getRequestURI(), ex);
        return ProblemResponses.of(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur inattendue est survenue. Communiquez l'identifiant de corrélation au support.",
                req.getRequestURI());
    }

    private static ProblemDetail validation(List<Map<String, String>> errors, HttpServletRequest req) {
        ProblemDetail pd = ProblemResponses.of(HttpStatus.BAD_REQUEST, "VALIDATION",
                "Certaines données sont invalides.", req.getRequestURI());
        pd.setProperty("errors", errors);
        return pd;
    }

    private static String lastNode(String path) {
        int i = path.lastIndexOf('.');
        return i < 0 ? path : path.substring(i + 1);
    }
}
