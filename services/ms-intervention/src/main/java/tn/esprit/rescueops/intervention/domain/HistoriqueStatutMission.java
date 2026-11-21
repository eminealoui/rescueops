
package tn.esprit.rescueops.intervention.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "historique_statut_mission")
@Getter
@Setter
public class HistoriqueStatutMission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", nullable = false)
    private StatutMission ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false)
    private StatutMission nouveauStatut;

    @Column(name = "date_changement", nullable = false)
    private Instant dateChangement;

    @Column(name = "modifie_par", nullable = false, length = 36)
    private String modifiePar;
}
