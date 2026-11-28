package tn.esprit.rescueops.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class AccessLogWebFilterTest {

    private final AccessLogWebFilter filter = new AccessLogWebFilter();

    @Test
    void laisseToujoursPasserLaRequete() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/hospitals/x"));
        exchange.getAttributes().put(CorrelationIdWebFilter.ATTRIBUTE, "abc12345-test");
        AtomicBoolean appele = new AtomicBoolean();
        WebFilterChain chain = ex -> {
            appele.set(true);
            ex.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(appele).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void ignoreLesCheminsHorsApi() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/actuator/health"));
        AtomicBoolean appele = new AtomicBoolean();

        filter.filter(exchange, ex -> { appele.set(true); return Mono.empty(); }).block();

        assertThat(appele).isTrue();
    }
}
