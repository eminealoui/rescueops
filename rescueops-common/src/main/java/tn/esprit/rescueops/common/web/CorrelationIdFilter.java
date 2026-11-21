package tn.esprit.rescueops.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.rescueops.common.error.ProblemResponses;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Identifiant de corrélation (BNF-09) : reprend {@code X-Correlation-Id} s'il est sûr, sinon en génère un,
 * le place dans le MDC (logs) et le renvoie dans la réponse.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9-]{8,64}$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String id = sanitize(request.getHeader(HEADER));
        MDC.put(ProblemResponses.MDC_CORRELATION_ID, id);
        response.setHeader(HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(ProblemResponses.MDC_CORRELATION_ID);
        }
    }

    /** Valeur reçue si elle respecte le format attendu, sinon un nouvel UUID (évite l'injection dans les logs). */
    public static String sanitize(String incoming) {
        return incoming != null && SAFE.matcher(incoming).matches() ? incoming : UUID.randomUUID().toString();
    }
}
