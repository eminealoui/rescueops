package tn.esprit.rescueops.emergency.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "historique_incident")
@Getter
@Setter
public class HistoriqueIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TypeActionIncident action;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 30)
    private StatutIncident ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", length = 30)
    private StatutIncident nouveauStatut;

    @Column(nullable = false, length = 100)
    private String auteur;

    @Column(length = 2000)
    private String details;

    @Column(name = "date_action", nullable = false)
    private Instant dateAction;
}