package tn.esprit.rescueops.hospital.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class DemandeTransfert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID missionId;

    @Enumerated(EnumType.STRING)
    private StatutTransfert statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private ServiceHospitalier serviceHospitalier;
}
