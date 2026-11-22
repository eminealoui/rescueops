
package tn.esprit.rescueops.intervention.web;

import tn.esprit.rescueops.intervention.dto.MissionRequest;
import tn.esprit.rescueops.intervention.dto.MissionResponse;
import tn.esprit.rescueops.intervention.service.MissionService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import tn.esprit.rescueops.intervention.dto.ItineraireRequest;
import tn.esprit.rescueops.intervention.dto.ItineraireResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import tn.esprit.rescueops.intervention.dto.ModifierMissionRequest;
import tn.esprit.rescueops.intervention.dto.MissionResponse;

import tn.esprit.rescueops.intervention.dto.ModifierMissionRequest;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rescueops.intervention.dto.ChangerStatutMissionRequest;
import java.util.UUID;

import java.util.List;
import tn.esprit.rescueops.intervention.dto.HistoriqueMissionResponse;

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
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'OPERATEUR', 'ADMIN')")
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

    @GetMapping
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'OPERATEUR', 'ADMIN')")
    public Page<MissionResponse> lister(
        @PageableDefault(
            size = 10,
            sort = "creeLe",
            direction = Sort.Direction.DESC
        ) Pageable pageable
    ) {
        return service.lister(pageable);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public MissionResponse modifier(
        @PathVariable UUID id,
        @Valid @RequestBody ModifierMissionRequest request
    ) {
        return service.modifier(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public void supprimer(@PathVariable UUID id) {
        service.supprimer(id);
    }

    @GetMapping("/{id}/historique")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'OPERATEUR', 'ADMIN')")
    public List<HistoriqueMissionResponse> historique(
        @PathVariable UUID id
    ) {
        return service.historique(id);
    }
    @PutMapping("/{id}/itineraire")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public ItineraireResponse modifierItineraire(
        @PathVariable UUID id,
        @Valid @RequestBody ItineraireRequest request
    ) {
        return service.modifierItineraire(id, request);
    }

    @GetMapping("/{id}/itineraire")
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'OPERATEUR', 'ADMIN')")
    public ItineraireResponse trouverItineraire(
        @PathVariable UUID id
    ) {
        return service.trouverItineraire(id);
    }

    @DeleteMapping("/{id}/itineraire")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('AMBULANCIER', 'ADMIN')")
    public void supprimerItineraire(
        @PathVariable UUID id
    ) {
        service.supprimerItineraire(id);
    }





}
