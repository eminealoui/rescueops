package tn.esprit.rescueops.dispatch.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Résultat du scoring multicritère pour un incident donné (BF-07).
 * Chaque ligne représente le score d'une ressource candidate pour un incident.
 * L'opérateur consulte ces recommandations classées avant de valider une Affectation.
 */
@Entity
@Table(name = "recommandation")
@Getter
@Setter
@NoArgsConstructor
public class Recommandation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String incidentId;

    @Column(nullable = false, length = 100)
    private String ressourceId;

    /** Nom ou libellé de la ressource (dénormalisé pour affichage). */
    @Column(length = 200)
    private String nomRessource;

    /** Type de ressource : AMBULANCE, EQUIPE, EQUIPEMENT. */
    @Column(length = 50)
    private String typeRessource;

    /** Score global entre 0.0 et 1.0. Plus le score est élevé, plus la ressource est adaptée. */
    @Column(nullable = false)
    private Double score;

    /** Justification lisible : « Distance 2.3km (40%), Disponible (40%), Compétences 100% (20%) ». */
    @Column(columnDefinition = "TEXT")
    private String justification;

    /** Distance en km entre la ressource et l'incident. */
    private Double distanceKm;

    /** Latitude de l'incident (pour affichage sur carte). */
    private Double latitudeIncident;

    /** Longitude de l'incident (pour affichage sur carte). */
    private Double longitudeIncident;

    /** La ressource est-elle disponible au moment du calcul ? */
    private Boolean disponible;

    /** La ressource a-t-elle toutes les compétences requises ? */
    private Boolean competencesOk;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
