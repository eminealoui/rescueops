package tn.esprit.rescueops.emergency.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "evenement_traite")
@Getter
@Setter
public class EvenementTraite {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(
            name = "event_type",
            nullable = false,
            length = 100
    )
    private String eventType;

    @Column(
            name = "date_traitement",
            nullable = false
    )
    private Instant dateTraitement;
}