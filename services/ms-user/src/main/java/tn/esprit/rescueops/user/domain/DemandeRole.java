package tn.esprit.rescueops.user.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.rescueops.common.security.Role;

import java.time.Instant;
import java.util.UUID;

/** Demande d'un rôle professionnel, en attente de validation par un ADMIN. */
@Entity
@Table(name = "demande_role")
@Getter
@Setter
public class DemandeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, length = 36)
    private String keycloakId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_demande", nullable = false, length = 40)
    private Role roleDemande;

    @Column(nullable = false, length = 150)
    private String organisation;

    @Column(length = 500)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDemandeRole statut;

    @Column(name = "traite_par", length = 36)
    private String traitePar;

    @Column(name = "motif_refus", length = 500)
    private String motifRefus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "traite_le")
    private Instant traiteLe;

    @Version
    private long version;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
