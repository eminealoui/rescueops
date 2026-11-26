package tn.esprit.rescueops.intervention.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Intervention métier (Module 4). Le schéma est créé par Flyway V2__creation_intervention.sql.
 * <p>
 * L'identifiant incident reste une clé externe : aucun join avec la base de ms-emergency.
 */
@Entity
@Table(name = "intervention")
@Getter
@Setter
@NoArgsConstructor
public class Intervention {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    /** Identifiant de l'incident d'origine, utilisé comme référence par ms-dispatch. */
    @Column(name = "incident_id", nullable = false, length = 100)
    private String incidentId;

    @Column(nullable = false, length = 200)
    private String titre;

    /** URGENCE, SECOURS, TRANSPORT... */
    @Column(name = "type_intervention", nullable = false, length = 50)
    private String typeIntervention;

    private Double latitude;

    private Double longitude;

    /** EN_ATTENTE, EN_COURS, TERMINEE, ANNULEE. */
    @Column(nullable = false, length = 30)
    private String statut = "EN_ATTENTE";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
