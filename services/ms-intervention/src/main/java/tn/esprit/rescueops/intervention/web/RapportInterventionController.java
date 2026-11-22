
package tn.esprit.rescueops.intervention.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

import tn.esprit.rescueops.intervention.dto.RapportRequest;
import tn.esprit.rescueops.intervention.dto.RapportResponse;
import tn.esprit.rescueops.intervention.service.RapportInterventionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/interventions/missions/{missionId}/rapport")
public class RapportInterventionController {

    private final RapportInterventionService service;

    public RapportInterventionController(
        RapportInterventionService service
    ) {
        this.service = service;
    }

    // Créer un rapport
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public RapportResponse creer(
        @PathVariable UUID missionId,
        @Valid @RequestBody RapportRequest request
    ) {
        return service.creer(missionId, request);
    }

    // Consulter le rapport d'une mission
    @GetMapping
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public RapportResponse trouver(
        @PathVariable UUID missionId
    ) {
        return service.trouver(missionId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public RapportResponse modifier(
        @PathVariable UUID missionId,
        @Valid @RequestBody RapportRequest request
    ) {
        return service.modifier(missionId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public void supprimer(@PathVariable UUID missionId) {
        service.supprimer(missionId);
    }


}
