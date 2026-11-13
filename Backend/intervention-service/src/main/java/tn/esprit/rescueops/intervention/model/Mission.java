package tn.esprit.rescueops.intervention.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Mission {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID affectationId;

    @Enumerated(EnumType.STRING)
    private StatutMission statut;

    private LocalDateTime dateDebut;

    private LocalDateTime dateFin;
}
