package tn.esprit.rescueops.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import tn.esprit.rescueops.common.error.ProblemResponses;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/** Réponses 401 et 403 au même format ProblemDetail que les autres erreurs. */
class ProblemDetailSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, ProblemResponses.of(HttpStatus.UNAUTHORIZED, "NON_AUTHENTIFIE",
                "Authentification requise : jeton absent, expiré ou invalide.", request.getRequestURI()));
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        write(response, ProblemResponses.of(HttpStatus.FORBIDDEN, "ACCES_REFUSE",
                "Vos droits ne permettent pas cette opération.", request.getRequestURI()));
    }

    private static void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        JSON.writeValue(response.getOutputStream(), ProblemResponses.toMap(problem));
    }
}
