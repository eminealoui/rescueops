package tn.esprit.rescueops.user.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.user.domain.StatutDemandeRole;
import tn.esprit.rescueops.user.dto.DemandeRoleRequest;
import tn.esprit.rescueops.user.dto.DemandeRoleResponse;
import tn.esprit.rescueops.user.dto.PageResponse;
import tn.esprit.rescueops.user.service.DemandeRoleService;

import java.util.List;
import java.util.UUID;

/** Demandes de rôle professionnel : demandées par l'utilisateur, accordées ou refusées par un ADMIN. */
@RestController
@RequestMapping("/api/users")
public class DemandeRoleController {

    public record RefusRequest(@Size(max = 500) String motif) {
    }

    private final DemandeRoleService service;

    public DemandeRoleController(DemandeRoleService service) {
        this.service = service;
    }

    @PostMapping("/me/role-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public DemandeRoleResponse demander(JwtAuthenticationToken auth, @Valid @RequestBody DemandeRoleRequest req) {
        return service.demander(CurrentUser.from(auth.getToken()), req);
    }

    @GetMapping("/me/role-requests")
    public List<DemandeRoleResponse> mesDemandes(JwtAuthenticationToken auth) {
        return service.mesDemandes(CurrentUser.from(auth.getToken()));
    }

    @GetMapping("/role-requests")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<DemandeRoleResponse> lister(@RequestParam(required = false) StatutDemandeRole statut,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return service.lister(statut, page, size);
    }

    @PostMapping("/role-requests/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public DemandeRoleResponse approuver(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return service.approuver(id, CurrentUser.from(auth.getToken()));
    }

    @PostMapping("/role-requests/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public DemandeRoleResponse refuser(@PathVariable UUID id, @Valid @RequestBody(required = false) RefusRequest req,
                                       JwtAuthenticationToken auth) {
        return service.refuser(id, req == null ? null : req.motif(), CurrentUser.from(auth.getToken()));
    }
}
