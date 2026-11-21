
package tn.esprit.rescueops.intervention.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mission")
@Getter
@Setter
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
        name = "commentaire",
        nullable = false,
        length = 500
    )
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutMission statut;

    @Column(
        name = "cree_par",
        nullable = false,
        length = 36
    )
    private String creePar;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Version
    private long version;

    @Column(
        name = "affectation_id",
        columnDefinition = "BINARY(16)",
        unique = true
    )
    private UUID affectationId;

    @Column(name = "incident_id", columnDefinition = "BINARY(16)")
    private UUID incidentId;

    @Column(name = "ambulance_id", columnDefinition = "BINARY(16)")
    private UUID ambulanceId;

    @Column(name = "ambulancier_keycloak_id", length = 36)
    private String ambulancierKeycloakId;

}
