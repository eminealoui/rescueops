package tn.esprit.rescueops.resource.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDateTime datePrevue;

    @Enumerated(EnumType.STRING)
    private StatutMaintenance statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Ambulance ambulance;
}
