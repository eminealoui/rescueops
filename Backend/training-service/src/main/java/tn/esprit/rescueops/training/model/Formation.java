package tn.esprit.rescueops.training.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Formation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String titre;

    private String prerequis;

    private String scenarioSimulation;

    private int niveauSimulation;
}
