package tn.esprit.rescueops.hospital.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** Mission close (reçue par mission.terminee) : bloque toute nouvelle demande de transfert pour elle. */
@Entity
@Table(name = "mission_terminee")
@Getter
@NoArgsConstructor
public class MissionTerminee {

    @Id
    @Column(name = "mission_id")
    private UUID missionId;

    @Column(name = "terminee_le", nullable = false)
    private Instant termineeLe;

    public MissionTerminee(UUID missionId, Instant termineeLe) {
        this.missionId = missionId;
        this.termineeLe = termineeLe;
    }
}
