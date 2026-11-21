
package tn.esprit.rescueops.intervention.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rapport_intervention")
@Getter
@Setter
public class RapportIntervention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "mission_id",
        nullable = false,
        unique = true
    )
    private Mission mission;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String bilan;

    @Column(
        name = "actions_realisees",
        nullable = false,
        columnDefinition = "TEXT"
    )
    private String actionsRealisees;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "cree_par", nullable = false, length = 36)
    private String creePar;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Version
    private long version;
}
