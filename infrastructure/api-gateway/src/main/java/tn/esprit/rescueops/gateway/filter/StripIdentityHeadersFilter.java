package tn.esprit.rescueops.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;

/**
 * Anti-usurpation : un client ne peut pas se faire passer pour quelqu'un d'autre en envoyant des en-têtes
 * d'identité. Les microservices ne font confiance qu'au JWT, jamais à ces en-têtes.
 */
@Component
public class StripIdentityHeadersFilter implements GlobalFilter, Ordered {

    private static final List<String> EXACT = List.of("x-user-id", "x-user-roles", "x-forwarded-user");
    private static final String PREFIX = "x-auth-request-";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange cleaned = exchange.mutate()
                .request(r -> r.headers(StripIdentityHeadersFilter::strip))
                .build();
        return chain.filter(cleaned);
    }

    private static void strip(HttpHeaders headers) {
        List<String> toRemove = headers.headerNames().stream()
                .filter(name -> {
                    String n = name.toLowerCase(Locale.ROOT);
                    return EXACT.contains(n) || n.startsWith(PREFIX);
                })
                .toList();
        toRemove.forEach(headers::remove);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
