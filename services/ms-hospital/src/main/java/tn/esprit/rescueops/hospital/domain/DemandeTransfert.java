package tn.esprit.rescueops.hospital.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Demande de transfert d'un patient. Le statut ne change JAMAIS par setter + save :
 * uniquement par les UPDATE conditionnels de DemandeTransfertRepository (une seule transition gagne).
 */
@Entity
@Table(name = "demande_transfert")
@Getter
@Setter
public class DemandeTransfert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Identifiant de la mission dans ms-intervention (pas de clé étrangère entre services). */
    @Column(name = "mission_id", nullable = false)
    private UUID missionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialite_requise", nullable = false, length = 30)
    private Specialite specialiteRequise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gravite gravite;

    @Column(name = "latitude_origine")
    private Double latitudeOrigine;

    @Column(name = "longitude_origine")
    private Double longitudeOrigine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceHospitalier service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutTransfert statut;

    @Column(name = "expire_at")
    private Instant expireAt;

    /** sub Keycloak de la personne qui a créé la demande. */
    @Column(name = "demandeur_user_id", nullable = false, length = 36)
    private String demandeurUserId;

    @Column(name = "traite_par_user_id", length = 36)
    private String traiteParUserId;

    @Column(name = "motif_refus", length = 500)
    private String motifRefus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }
}
