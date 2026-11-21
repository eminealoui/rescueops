package tn.esprit.rescueops.gateway.error;

import io.netty.channel.ConnectTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/**
 * Toutes les erreurs de routage deviennent des ProblemDetail (BNF-07) :
 * service absent d'Eureka → 503, service injoignable → 503, délai dépassé → 504, route inconnue → 404.
 */
@Component
@Order(-2)
public class GatewayProblemHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GatewayProblemHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        String path = exchange.getRequest().getPath().value();

        if (ex instanceof TimeoutException || causedBy(ex, TimeoutException.class)
                || (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() == 504)) {
            return Problems.write(exchange, HttpStatus.GATEWAY_TIMEOUT, "DELAI_DEPASSE",
                    "Le service " + serviceOf(path) + " n'a pas répondu à temps.");
        }
        if (causedBy(ex, ConnectException.class) || causedBy(ex, ConnectTimeoutException.class)
                || (ex instanceof ResponseStatusException rse && rse.getStatusCode().value() == 503)) {
            return Problems.write(exchange, HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_INDISPONIBLE",
                    "Le service " + serviceOf(path) + " est momentanément indisponible.");
        }
        if (ex instanceof ResponseStatusException rse && rse.getStatusCode().is4xxClientError()) {
            HttpStatus status = HttpStatus.valueOf(rse.getStatusCode().value());
            String code = status == HttpStatus.NOT_FOUND ? "RESSOURCE_INTROUVABLE" : status.name();
            return Problems.write(exchange, status, code,
                    status == HttpStatus.NOT_FOUND ? "Aucune route ne correspond à " + path + "." : rse.getReason());
        }
        log.error("Erreur inattendue dans la Gateway sur {}", path, ex);
        return Problems.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur inattendue est survenue. Communiquez l'identifiant de corrélation au support.");
    }

    /** {@code /api/hospitals/12} → {@code ms-hospital} : nom lisible du service visé. */
    static String serviceOf(String path) {
        String[] parts = path.split("/");
        String resource = parts.length > 2 ? parts[2] : "";
        return switch (resource) {
            case "users" -> "ms-user";
            case "hospitals", "transfers" -> "ms-hospital";
            case "emergencies" -> "ms-emergency";
            case "resources" -> "ms-resource";
            case "dispatch" -> "ms-dispatch";
            case "interventions" -> "ms-intervention";
            case "trainings" -> "ms-training";
            default -> "demandé";
        };
    }

    private static boolean causedBy(Throwable ex, Class<? extends Throwable> type) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (type.isInstance(t)) {
                return true;
            }
        }
        return false;
    }
}
