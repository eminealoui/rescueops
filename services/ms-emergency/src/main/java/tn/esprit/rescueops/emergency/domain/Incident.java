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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "incident")
@Getter
@Setter
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 150)
    private String titre;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private TypeIncident type;

    @Column(length = 255)
    private String adresse;

    private Double latitude;

    private Double longitude;

    @Column(name = "nombre_victimes")
    private Integer nombreVictimes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatutIncident statut;

    @Column(name = "cree_par", nullable = false, length = 36)
    private String creePar;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Version
    private long version;

    @Column(name = "traite_par", length = 36)
    private String traitePar;

    @Column(name = "traite_le")
    private Instant traiteLe;

    @Column(name = "motif_rejet", length = 500)
    private String motifRejet;

    @Column(name = "cloture_par", length = 36)
    private String cloturePar;

    @Column(name = "cloture_le")
    private Instant clotureLe;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NiveauGravite gravite;

    @Column(name = "danger_immediat")
    private Boolean dangerImmediat;

    @Column(name = "personnes_vulnerables")
    private Boolean personnesVulnerables;

    @Column(name = "score_priorite")
    private Integer scorePriorite;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private NiveauPriorite priorite;

    @Enumerated(EnumType.STRING)
    @Column(name = "priorite_suggeree", length = 30)
    private NiveauPriorite prioriteSuggeree;

    @Column(name = "explication_priorite", length = 1000)
    private String explicationPriorite;

    @Column(name = "motif_ajustement_priorite", length = 500)
    private String motifAjustementPriorite;

    @Column(name = "incident_principal_id")
    private UUID incidentPrincipalId;

    @Column(name = "fusionne_par", length = 36)
    private String fusionnePar;

    @Column(name = "fusionne_le")
    private Instant fusionneLe;

    @Column(name = "motif_fusion", length = 500)
    private String motifFusion;
}