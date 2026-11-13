package tn.esprit.rescueops.training.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SessionFormation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDateTime dateDebut;

    private int capacite;

    @ManyToOne(fetch = FetchType.LAZY)
    private Formation formation;
}
