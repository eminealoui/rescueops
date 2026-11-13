package tn.esprit.rescueops.resource.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Ambulance {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String immatriculation;

    @Enumerated(EnumType.STRING)
    private Disponibilite statut;

    private String position;
}
