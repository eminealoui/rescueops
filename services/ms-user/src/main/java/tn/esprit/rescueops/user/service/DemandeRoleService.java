package tn.esprit.rescueops.user.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.common.error.BadRequestException;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.user.domain.DemandeRole;
import tn.esprit.rescueops.user.domain.StatutDemandeRole;
import tn.esprit.rescueops.user.dto.DemandeRoleRequest;
import tn.esprit.rescueops.user.dto.DemandeRoleResponse;
import tn.esprit.rescueops.user.dto.PageResponse;
import tn.esprit.rescueops.user.keycloak.KeycloakAdminClient;
import tn.esprit.rescueops.user.repository.DemandeRoleRepository;
import tn.esprit.rescueops.user.repository.UserProfileRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Rôles professionnels : l'inscription publique donne CITOYEN ; un rôle professionnel est demandé ici
 * puis accordé par un ADMIN. Keycloak reste la seule autorité : le rôle n'est stocké QUE dans Keycloak.
 */
@Service
public class DemandeRoleService {

    /** ADMIN ne se demande jamais ; CITOYEN est déjà donné à l'inscription. */
    static final Set<Role> DEMANDABLES = Set.of(Role.OPERATEUR, Role.GESTIONNAIRE_RESSOURCES, Role.AMBULANCIER,
            Role.COORDINATEUR_HOSPITALIER, Role.FORMATEUR, Role.APPRENANT);

    private final DemandeRoleRepository demandes;
    private final UserProfileRepository profils;
    private final KeycloakAdminClient keycloak;

    public DemandeRoleService(DemandeRoleRepository demandes, UserProfileRepository profils, KeycloakAdminClient keycloak) {
        this.demandes = demandes;
        this.profils = profils;
        this.keycloak = keycloak;
    }

    @Transactional
    public DemandeRoleResponse demander(CurrentUser user, DemandeRoleRequest req) {
        if (!DEMANDABLES.contains(req.role())) {
            throw new BadRequestException("ROLE_NON_DEMANDABLE", "Le rôle " + req.role() + " ne peut pas être demandé.");
        }
        if (user.has(req.role())) {
            throw new ConflictException("ROLE_DEJA_ATTRIBUE", "Vous avez déjà le rôle " + req.role() + ".");
        }
        if (demandes.existsByKeycloakIdAndStatut(user.keycloakId(), StatutDemandeRole.EN_ATTENTE)) {
            throw new ConflictException("DEMANDE_EN_ATTENTE", "Vous avez déjà une demande en attente de validation.");
        }
        DemandeRole d = new DemandeRole();
        d.setKeycloakId(user.keycloakId());
        d.setRoleDemande(req.role());
        d.setOrganisation(req.organisation().trim());
        d.setMotif(req.motif() == null || req.motif().isBlank() ? null : req.motif().trim());
        d.setStatut(StatutDemandeRole.EN_ATTENTE);
        return DemandeRoleResponse.of(demandes.save(d));
    }

    @Transactional(readOnly = true)
    public List<DemandeRoleResponse> mesDemandes(CurrentUser user) {
        return demandes.findByKeycloakIdOrderByCreatedAtDesc(user.keycloakId()).stream().map(DemandeRoleResponse::of).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DemandeRoleResponse> lister(StatutDemandeRole statut, int page, int size) {
        var p = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by("createdAt").ascending());
        var resultat = statut == null ? demandes.findAll(p) : demandes.findByStatut(statut, p);
        return PageResponse.of(resultat, DemandeRoleResponse::of);
    }

    /**
     * Keycloak D'ABORD, la base ENSUITE : si Keycloak échoue, rien n'est modifié (503) et l'admin peut réessayer ;
     * si la base échoue après Keycloak, une nouvelle approbation est sans danger (attribution idempotente).
     */
    @Transactional
    public DemandeRoleResponse approuver(UUID id, CurrentUser admin) {
        DemandeRole d = enAttente(id);
        keycloak.attribuerRole(d.getKeycloakId(), d.getRoleDemande().name());
        if (demandes.cloturer(id, StatutDemandeRole.ACCEPTEE, admin.keycloakId(), null, Instant.now()) == 0) {
            throw dejaTraitee();
        }
        profils.findByKeycloakId(d.getKeycloakId()).ifPresent(p -> {
            if (p.getOrganisation() == null) {
                p.setOrganisation(d.getOrganisation());
            }
        });
        return DemandeRoleResponse.of(demandes.findById(id).orElseThrow());
    }

    @Transactional
    public DemandeRoleResponse refuser(UUID id, String motif, CurrentUser admin) {
        enAttente(id);
        if (demandes.cloturer(id, StatutDemandeRole.REFUSEE, admin.keycloakId(), motif, Instant.now()) == 0) {
            throw dejaTraitee();
        }
        return DemandeRoleResponse.of(demandes.findById(id).orElseThrow());
    }

    private DemandeRole enAttente(UUID id) {
        DemandeRole d = demandes.findById(id).orElseThrow(() -> new NotFoundException("Demande de rôle introuvable."));
        if (d.getStatut() != StatutDemandeRole.EN_ATTENTE) {
            throw dejaTraitee();
        }
        return d;
    }

    private static ConflictException dejaTraitee() {
        return new ConflictException("DEMANDE_DEJA_TRAITEE", "Cette demande a déjà été traitée.");
    }
}
