package tn.esprit.rescueops.hospital.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Service d'un hôpital et sa capacité.
 * ATTENTION : placesDisponibles ne se modifie JAMAIS par setter + save (risque de double réservation),
 * uniquement par les UPDATE conditionnels de ServiceHospitalierRepository.
 */
@Entity
@Table(name = "service_hospitalier")
@Getter
@Setter
public class ServiceHospitalier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hopital_id", nullable = false)
    private Hopital hopital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Specialite specialite;

    @Column(name = "capacite_totale", nullable = false)
    private int capaciteTotale;

    @Column(name = "places_disponibles", nullable = false)
    private int placesDisponibles;

    @Column(nullable = false)
    private boolean actif = true;

    @Version
    private long version;
}
