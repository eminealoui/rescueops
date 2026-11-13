package tn.esprit.rescueops.training.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Certification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID titulaireUserId;

    private LocalDate valideJusquAu;

    @ManyToOne(fetch = FetchType.LAZY)
    private Evaluation evaluation;
}
