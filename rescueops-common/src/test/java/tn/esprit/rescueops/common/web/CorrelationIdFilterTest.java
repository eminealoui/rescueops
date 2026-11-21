package tn.esprit.rescueops.common.web;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private String run(String incoming, AtomicReference<String> seenInMdc) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/x");
        if (incoming != null) {
            req.addHeader(CorrelationIdFilter.HEADER, incoming);
        }
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest r, jakarta.servlet.ServletResponse s) {
                seenInMdc.set(MDC.get("correlationId"));
            }
        });
        return res.getHeader(CorrelationIdFilter.HEADER);
    }

    @Test
    void reprendLIdentifiantValideFourniParLeClient() throws Exception {
        AtomicReference<String> mdc = new AtomicReference<>();

        String returned = run("abc12345-test", mdc);

        assertThat(returned).isEqualTo("abc12345-test");
        assertThat(mdc.get()).isEqualTo("abc12345-test");
    }

    @Test
    void genereUnUuidSiAbsent() throws Exception {
        AtomicReference<String> mdc = new AtomicReference<>();

        String returned = run(null, mdc);

        assertThat(UUID.fromString(returned)).isNotNull();
        assertThat(mdc.get()).isEqualTo(returned);
    }

    @Test
    void remplaceUnIdentifiantDangereux() throws Exception {
        AtomicReference<String> mdc = new AtomicReference<>();

        String returned = run("<script>alert(1)</script>", mdc);

        assertThat(returned).doesNotContain("<");
        assertThat(UUID.fromString(returned)).isNotNull();
    }

    @Test
    void nettoieLeMdcApresLaRequete() throws Exception {
        run("abc12345-test", new AtomicReference<>());

        assertThat(MDC.get("correlationId")).isNull();
    }
}
