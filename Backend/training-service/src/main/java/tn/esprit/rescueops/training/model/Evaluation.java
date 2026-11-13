package tn.esprit.rescueops.training.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Evaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private int scoreMinimal;

    @ManyToOne(fetch = FetchType.LAZY)
    private Formation formation;
}
