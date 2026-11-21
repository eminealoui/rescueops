package tn.esprit.rescueops.hospital.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Trace de chaque ajustement manuel de capacité (BF-20). */
@Entity
@Table(name = "historique_capacite")
@Getter
@Setter
public class HistoriqueCapacite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_id", nullable = false)
    private UUID serviceId;

    @Column(name = "ancienne_capacite", nullable = false)
    private int ancienneCapacite;

    @Column(name = "nouvelle_capacite", nullable = false)
    private int nouvelleCapacite;

    @Column(name = "anciennes_places", nullable = false)
    private int anciennesPlaces;

    @Column(name = "nouvelles_places", nullable = false)
    private int nouvellesPlaces;

    @Column(name = "acteur_user_id", nullable = false, length = 36)
    private String acteurUserId;

    @Column(nullable = false)
    private Instant horodatage;

    @Column(length = 500)
    private String motif;
}
