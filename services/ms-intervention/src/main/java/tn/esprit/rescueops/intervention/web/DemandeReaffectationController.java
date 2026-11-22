
package tn.esprit.rescueops.intervention.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import tn.esprit.rescueops.intervention.dto.*;
import tn.esprit.rescueops.intervention.service.DemandeReaffectationService;

import java.util.UUID;

@RestController
@RequestMapping("/api/interventions")
public class DemandeReaffectationController {

    private final DemandeReaffectationService service;

    public DemandeReaffectationController(
        DemandeReaffectationService service
    ) {
        this.service = service;
    }

    @PostMapping("/missions/{missionId}/reaffectations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AMBULANCIER')")
    public DemandeReaffectationResponse creer(
        @PathVariable UUID missionId,
        @Valid @RequestBody DemandeReaffectationRequest request
    ) {
        return service.creer(missionId, request);
    }

    @GetMapping("/reaffectations/{id}")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public DemandeReaffectationResponse trouver(
        @PathVariable UUID id
    ) {
        return service.trouver(id);
    }

    @GetMapping("/missions/{missionId}/reaffectations")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public List<DemandeReaffectationResponse> lister(
        @PathVariable UUID missionId
    ) {
        return service.lister(missionId);
    }

    @PutMapping("/reaffectations/{id}")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public DemandeReaffectationResponse modifier(
        @PathVariable UUID id,
        @Valid @RequestBody DemandeReaffectationRequest request
    ) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/reaffectations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public void annuler(@PathVariable UUID id) {
        service.annuler(id);
    }


}
