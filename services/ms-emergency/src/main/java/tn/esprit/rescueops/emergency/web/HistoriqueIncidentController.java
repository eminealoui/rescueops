package tn.esprit.rescueops.emergency.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tn.esprit.rescueops.emergency.dto.HistoriqueIncidentResponse;
import tn.esprit.rescueops.emergency.service.HistoriqueIncidentService;

@RestController
@RequestMapping("/api/emergencies")
public class HistoriqueIncidentController {

    private final HistoriqueIncidentService service;

    public HistoriqueIncidentController(
            HistoriqueIncidentService service
    ) {
        this.service = service;
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public List<HistoriqueIncidentResponse> consulter(
            @PathVariable UUID id
    ) {

        return service.consulter(id);
    }
}