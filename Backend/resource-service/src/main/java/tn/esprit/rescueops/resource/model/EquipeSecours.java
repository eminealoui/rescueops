package tn.esprit.rescueops.resource.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class EquipeSecours {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nom;

    private UUID chefUserId;

    @Enumerated(EnumType.STRING)
    private StatutEquipe statut;

    @ManyToOne(fetch = FetchType.LAZY)
    private Ambulance ambulance;
}
