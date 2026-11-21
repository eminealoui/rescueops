package tn.esprit.rescueops.hospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** eventId déjà traité : garantit qu'un message livré deux fois n'a qu'un seul effet (BNF-08). */
@Entity
@Table(name = "evenement_traite")
@Getter
@NoArgsConstructor
public class EvenementTraite {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "traite_le", nullable = false)
    private Instant traiteLe;

    public EvenementTraite(UUID eventId, Instant traiteLe) {
        this.eventId = eventId;
        this.traiteLe = traiteLe;
    }
}
