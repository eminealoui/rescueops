
package tn.esprit.rescueops.intervention.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "evenement_affectation_recu")
@Getter
@Setter
public class EvenementAffectationRecu {

    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "affectation_id", nullable = false)
    private UUID affectationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json",
        nullable = false,
        columnDefinition = "json")
    private Map<String, Object> payloadJson;

    @Column(name = "recu_le", nullable = false)
    private Instant recuLe;

    @Column(name = "traite", nullable = false)
    private boolean traite = false;
}
