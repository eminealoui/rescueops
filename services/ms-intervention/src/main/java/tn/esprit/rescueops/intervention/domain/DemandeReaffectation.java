
package tn.esprit.rescueops.intervention.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "demande_reaffectation")
@Getter
@Setter
public class DemandeReaffectation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column(nullable = false, length = 500)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutReaffectation statut;

    @Column(name = "demande_par",
        nullable = false, length = 36)
    private String demandePar;

    @Column(name = "date_demande", nullable = false)
    private Instant dateDemande;

    @Column(name = "date_traitement")
    private Instant dateTraitement;

    @Version
    private long version;
}
