package tn.esprit.rescueops.user.dto;

import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.user.domain.DemandeRole;
import tn.esprit.rescueops.user.domain.StatutDemandeRole;

import java.time.Instant;
import java.util.UUID;

public record DemandeRoleResponse(UUID id, String keycloakId, Role roleDemande, String organisation, String motif,
                                  StatutDemandeRole statut, String motifRefus, Instant createdAt, Instant traiteLe) {

    public static DemandeRoleResponse of(DemandeRole d) {
        return new DemandeRoleResponse(d.getId(), d.getKeycloakId(), d.getRoleDemande(), d.getOrganisation(),
                d.getMotif(), d.getStatut(), d.getMotifRefus(), d.getCreatedAt(), d.getTraiteLe());
    }
}
