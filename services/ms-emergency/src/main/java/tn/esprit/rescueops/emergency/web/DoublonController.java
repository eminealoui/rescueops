package tn.esprit.rescueops.emergency.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tn.esprit.rescueops.emergency.dto.DoublonResponse;
import tn.esprit.rescueops.emergency.service.DetectionDoublonService;

@RestController
@RequestMapping("/api/emergencies")
public class DoublonController {

    private final DetectionDoublonService service;

    public DoublonController(
            DetectionDoublonService service
    ) {
        this.service = service;
    }

    @GetMapping("/{id}/doublons")
    @PreAuthorize("hasAnyRole('OPERATEUR', 'ADMIN')")
    public List<DoublonResponse> detecter(
            @PathVariable UUID id
    ) {

        return service.detecter(id);
    }
}