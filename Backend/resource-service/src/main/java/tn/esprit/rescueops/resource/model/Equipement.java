package tn.esprit.rescueops.resource.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Equipement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nom;

    @Enumerated(EnumType.STRING)
    private EtatEquipement etat;

    @ManyToOne(fetch = FetchType.LAZY)
    private Ambulance ambulance;
}
