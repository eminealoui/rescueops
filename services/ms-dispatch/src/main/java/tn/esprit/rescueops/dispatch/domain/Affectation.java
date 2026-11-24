package tn.esprit.rescueops.dispatch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Affectation d'une ressource (ambulance, équipe) à un incident.
 * BF-08 : validée par l'opérateur via AffectationController.
 * BF-09 : annulée en cas de conflit, remplacée par une alternative.
 * <p>
 * Contrat Flyway : schéma créé dans V2__creation_affectation.sql. Ne pas utiliser ddl-auto: update.
 */
@Entity
@Table(name = "affectation")
@Getter
@Setter
@NoArgsConstructor
public class Affectation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'incident (clé externe, ne pas joindre la DB de ms-emergency). */
    @Column(nullable = false, length = 100)
    private String incidentId;

    /** Identifiant de la ressource (clé externe, ne pas joindre la DB de ms-resource). */
    @Column(nullable = false, length = 100)
    private String ressourceId;

    /** Type de ressource : AMBULANCE, EQUIPE, EQUIPEMENT. */
    @Column(length = 50)
    private String typeRessource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutAffectation statut = StatutAffectation.PROPOSEE;

    /** Score multicritère calculé par l'algorithme de scoring (BF-07), entre 0 et 1. */
    private Double score;

    /** Justification lisible du score : distance, disponibilité, compétences. */
    @Column(columnDefinition = "TEXT")
    private String justification;

    /** keycloakId de l'opérateur qui a validé ou annulé (null si PROPOSEE). */
    @Column(length = 100)
    private String operateurId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
