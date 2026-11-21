package tn.esprit.rescueops.hospital.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/** Trace de chaque changement de statut d'une demande (BF-20). */
@Entity
@Table(name = "historique_transfert")
@Getter
@Setter
public class HistoriqueTransfert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "demande_id", nullable = false)
    private UUID demandeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 20)
    private StatutTransfert ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false, length = 20)
    private StatutTransfert nouveauStatut;

    @Column(name = "acteur_user_id", nullable = false, length = 36)
    private String acteurUserId;

    @Column(nullable = false)
    private Instant horodatage;

    @Column(length = 500)
    private String commentaire;
}
