
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
    @Column(name = "transfert_id")
    private UUID transfertId;

    @Column(name = "service_hospitalier_id")
    private UUID serviceHospitalierId;

    @Column(name = "destination_confirmee_le")
    private Instant destinationConfirmeeLe;
    @Enumerated(EnumType.STRING)
    @Column(name = "type_itineraire", length = 30)
    private TypeItineraire typeItineraire;

    @Column(name = "latitude_depart")
    private Double latitudeDepart;

    @Column(name = "longitude_depart")
    private Double longitudeDepart;

    @Column(name = "latitude_destination")
    private Double latitudeDestination;

    @Column(name = "longitude_destination")
    private Double longitudeDestination;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "duree_estimee_minutes")
    private Integer dureeEstimeeMinutes;

    @Column(name = "itineraire_modifie_le")
    private Instant itineraireModifieLe;

}
