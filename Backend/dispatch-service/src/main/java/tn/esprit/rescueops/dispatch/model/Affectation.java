package tn.esprit.rescueops.dispatch.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Affectation {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID incidentId;

    private UUID ambulanceId;

    @Enumerated(EnumType.STRING)
    private StatutAffectation statut;
}
