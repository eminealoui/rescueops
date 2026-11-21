package tn.esprit.rescueops.common.event;

import org.slf4j.MDC;
import tn.esprit.rescueops.common.error.ProblemResponses;

import java.time.Instant;
import java.util.UUID;

/**
 * Enveloppe versionnée de tous les événements publiés sur {@code rescueops.events}.
 * Le consommateur s'appuie sur {@code eventId} pour ne jamais traiter deux fois le même événement (BNF-08).
 * Règle : aucun secret ni donnée personnelle inutile dans {@code payload}.
 *
 * @param eventType clé de routage, par ex. {@code transfert.confirme}
 */
public record RescueOpsEvent<T>(UUID eventId, String eventType, int version, Instant occurredAt,
                                String correlationId, T payload) {

    public static <T> RescueOpsEvent<T> of(String eventType, T payload) {
        return new RescueOpsEvent<>(UUID.randomUUID(), eventType, 1, Instant.now(),
                MDC.get(ProblemResponses.MDC_CORRELATION_ID), payload);
    }
}
