package tn.esprit.rescueops.gateway.error;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tn.esprit.rescueops.gateway.filter.CorrelationIdWebFilter;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** Écriture des erreurs de la Gateway au même format RFC 9457 que les microservices. */
public final class Problems {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private Problems() {
    }

    public static Mono<Void> write(ServerWebExchange exchange, HttpStatus status, String code, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "https://rescueops.tn/errors/" + code.toLowerCase().replace('_', '-'));
        body.put("title", status.getReasonPhrase());
        body.put("status", status.value());
        body.put("detail", detail);
        body.put("instance", exchange.getRequest().getPath().value());
        body.put("code", code);
        Object correlationId = exchange.getAttribute(CorrelationIdWebFilter.ATTRIBUTE);
        if (correlationId != null) {
            body.put("correlationId", correlationId);
        }
        var response = exchange.getResponse();
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        DataBuffer buffer = response.bufferFactory().wrap(JSON.writeValueAsBytes(body));
        return response.writeWith(Mono.just(buffer));
    }
}
