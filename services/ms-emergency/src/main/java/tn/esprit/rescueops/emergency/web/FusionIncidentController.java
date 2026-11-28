package tn.esprit.rescueops.emergency.web;

import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tn.esprit.rescueops.emergency.dto.FusionIncidentRequest;
import tn.esprit.rescueops.emergency.dto.FusionIncidentResponse;
import tn.esprit.rescueops.emergency.service.FusionIncidentService;

@RestController
@RequestMapping("/api/emergencies")
public class FusionIncidentController {

    private final FusionIncidentService service;

    public FusionIncidentController(
            FusionIncidentService service
    ) {
        this.service = service;
    }

    @PatchMapping("/{id}/fusionner")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public FusionIncidentResponse fusionner(

            @PathVariable UUID id,

            @Valid @RequestBody
            FusionIncidentRequest request
    ) {

        return service.fusionner(id, request);
    }
}