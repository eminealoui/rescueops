package tn.esprit.rescueops.hospital.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class ServiceHospitalier {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String specialite;

    private int placesDisponibles;

    @ManyToOne(fetch = FetchType.LAZY)
    private Hopital hopital;
}
