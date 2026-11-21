package tn.esprit.rescueops.common.error;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fabrique unique des réponses d'erreur RFC 9457 : tous les services renvoient les mêmes champs
 * ({@code type}, {@code title}, {@code status}, {@code detail}, {@code instance}, {@code code}, {@code correlationId}).
 */
public final class ProblemResponses {

    public static final String MDC_CORRELATION_ID = "correlationId";

    private ProblemResponses() {
    }

    public static ProblemDetail of(HttpStatus status, String code, String detail, String path) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setType(URI.create("https://rescueops.tn/errors/" + code.toLowerCase().replace('_', '-')));
        if (path != null) {
            pd.setInstance(URI.create(path));
        }
        pd.setProperty("code", code);
        String correlationId = MDC.get(MDC_CORRELATION_ID);
        if (correlationId != null) {
            pd.setProperty("correlationId", correlationId);
        }
        return pd;
    }

    /**
     * Forme JSON « à plat » d'un ProblemDetail (propriétés au premier niveau, comme le fait Spring MVC),
     * pour les composants qui écrivent la réponse eux-mêmes (filtres de sécurité).
     */
    public static Map<String, Object> toMap(ProblemDetail pd) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", String.valueOf(pd.getType()));
        body.put("title", pd.getTitle());
        body.put("status", pd.getStatus());
        body.put("detail", pd.getDetail());
        if (pd.getInstance() != null) {
            body.put("instance", pd.getInstance().toString());
        }
        if (pd.getProperties() != null) {
            body.putAll(pd.getProperties());
        }
        return body;
    }
}
