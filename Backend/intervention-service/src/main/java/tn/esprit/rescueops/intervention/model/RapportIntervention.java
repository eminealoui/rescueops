package tn.esprit.rescueops.intervention.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class RapportIntervention {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String bilan;

    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    private Mission mission;
}
