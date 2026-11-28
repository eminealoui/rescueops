package tn.esprit.rescueops.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Journal d'accès de la Gateway : une ligne par requête /api avec son identifiant de corrélation,
 * le statut et la durée. Avec Loki, on retrouve ainsi une requête de Nginx jusqu'à la Gateway.
 * Jamais de jeton ni de paramètre de requête dans le log : seulement la méthode et le chemin.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AccessLogWebFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger("rescueops.access");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (!path.startsWith("/api/")) {
            return chain.filter(exchange);
        }
        long debut = System.nanoTime();
        return chain.filter(exchange).doFinally(signal -> {
            HttpStatusCode status = exchange.getResponse().getStatusCode();
            long ms = (System.nanoTime() - debut) / 1_000_000;
            log.info("correlationId={} {} {} -> {} ({} ms)",
                    exchange.getAttribute(CorrelationIdWebFilter.ATTRIBUTE),
                    exchange.getRequest().getMethod(), path,
                    status != null ? status.value() : "-", ms);
        });
    }
}
