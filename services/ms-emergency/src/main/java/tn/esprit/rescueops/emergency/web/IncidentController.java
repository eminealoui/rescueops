package tn.esprit.rescueops.emergency.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeIncident;
import tn.esprit.rescueops.emergency.dto.IncidentRequest;
import tn.esprit.rescueops.emergency.dto.IncidentResponse;
import tn.esprit.rescueops.emergency.dto.PageResponse;
import tn.esprit.rescueops.emergency.dto.RejetIncidentRequest;
import tn.esprit.rescueops.emergency.dto.ResultatPriorisation;
import tn.esprit.rescueops.emergency.dto.ValidationIncidentRequest;
import tn.esprit.rescueops.emergency.service.IncidentService;
import tn.esprit.rescueops.emergency.service.PriorisationService;

@RestController
@RequestMapping("/api/emergencies")
public class IncidentController {

    private final IncidentService service;

    private final PriorisationService priorisationService;

    public IncidentController(
        IncidentService service,
        PriorisationService priorisationService
    ) {

        this.service = service;
        this.priorisationService = priorisationService;
    }

    // Déclarer un incident
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CITOYEN', 'OPERATEUR', 'ADMIN')")
    public IncidentResponse creer(
            @Valid @RequestBody IncidentRequest request
    ) {

        return service.creer(request);
    }

    // Consulter un incident
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CITOYEN', 'OPERATEUR', 'ADMIN')")
    public IncidentResponse trouver(@PathVariable UUID id) {

        return service.trouver(id);
    }

    // Liste et recherche multicritère
    @GetMapping
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public PageResponse<IncidentResponse> rechercher(

            @RequestParam(required = false)
            StatutIncident statut,

            @RequestParam(required = false)
            TypeIncident type,

            @RequestParam(required = false)
            String q,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        return service.rechercher(
                statut,
                type,
                q,
                page,
                size
        );
    }

    // Consulter mes propres incidents
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CITOYEN', 'OPERATEUR', 'ADMIN')")
    public PageResponse<IncidentResponse> mesIncidents(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size
    ) {

        return service.mesIncidents(page, size);
    }

    @PatchMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public IncidentResponse valider(
            @PathVariable UUID id,
            @Valid @RequestBody ValidationIncidentRequest request
    ) {

        return service.valider(id, request);
    }

    @PatchMapping("/{id}/rejeter")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public IncidentResponse rejeter(
            @PathVariable UUID id,
            @Valid @RequestBody RejetIncidentRequest request
    ) {

        return service.rejeter(id, request);
    }

    @PatchMapping("/{id}/cloturer")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public IncidentResponse cloturer(
            @PathVariable UUID id
    ) {

        return service.cloturer(id);
    }

    @PostMapping("/simuler-priorite")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public ResultatPriorisation simulerPriorite(
            @Valid @RequestBody ValidationIncidentRequest request
    ) {

        return priorisationService.calculer(
                request.type(),
                request.gravite(),
                request.nombreVictimes(),
                request.dangerImmediat(),
                request.personnesVulnerables()
        );
    }
}