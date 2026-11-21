package tn.esprit.rescueops.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identifiant de corrélation à l'entrée du système (BNF-09) : validé ou généré, transmis au microservice
 * dans {@code X-Correlation-Id} et renvoyé au client. Placé avant la sécurité pour que même un 401 le porte.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdWebFilter implements WebFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String ATTRIBUTE = "rescueops.correlationId";
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String id = incoming != null && SAFE.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();

        exchange.getAttributes().put(ATTRIBUTE, id);
        exchange.getResponse().getHeaders().set(HEADER, id);
        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.headers(h -> h.set(HEADER, id)))
                .build();
        return chain.filter(mutated);
    }
}
