
package tn.esprit.rescueops.intervention.web;

import tn.esprit.rescueops.intervention.dto.MissionRequest;
import tn.esprit.rescueops.intervention.dto.MissionResponse;
import tn.esprit.rescueops.intervention.service.MissionService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rescueops.intervention.dto.ChangerStatutMissionRequest;
import java.util.UUID;

@RestController
@RequestMapping("/api/interventions/missions")
public class MissionController {

    private final MissionService service;

    public MissionController(MissionService service) {
        this.service = service;
    }

    // Créer une mission
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public MissionResponse creer(
        @Valid @RequestBody MissionRequest request) {
        return service.creer(request);
    }

    // Consulter une mission par son ID
    @GetMapping("/{id}")
    public MissionResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public MissionResponse changerStatut(
        @PathVariable UUID id,
        @Valid @RequestBody ChangerStatutMissionRequest request
    ) {
        return service.changerStatut(id, request);
    }

}
