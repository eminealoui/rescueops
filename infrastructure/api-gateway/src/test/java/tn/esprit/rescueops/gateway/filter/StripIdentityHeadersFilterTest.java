package tn.esprit.rescueops.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class StripIdentityHeadersFilterTest {

    @Test
    void lesEntetesDIdentiteEnvoyesParLeClientSontSupprimes() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/users/me")
                .header("X-User-Id", "admin")
                .header("X-User-Roles", "ADMIN")
                .header("X-Forwarded-User", "admin")
                .header("X-Auth-Request-Email", "admin@x")
                .header("Authorization", "Bearer abc")
                .header("X-Correlation-Id", "abc12345-ok"));
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        new StripIdentityHeadersFilter().filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        var headers = forwarded.get().getRequest().getHeaders();
        assertThat(headers.containsHeader("X-User-Id")).isFalse();
        assertThat(headers.containsHeader("X-User-Roles")).isFalse();
        assertThat(headers.containsHeader("X-Forwarded-User")).isFalse();
        assertThat(headers.containsHeader("X-Auth-Request-Email")).isFalse();
        assertThat(headers.getFirst("Authorization")).isEqualTo("Bearer abc");
        assertThat(headers.getFirst("X-Correlation-Id")).isEqualTo("abc12345-ok");
    }
}
