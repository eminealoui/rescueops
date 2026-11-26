package tn.esprit.rescueops.intervention.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.intervention.domain.Intervention;
import tn.esprit.rescueops.intervention.repository.InterventionRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CRUD des interventions (Module 4).
 * <p>
 * Préfixe : {@code /api/interventions} — route déjà déclarée dans la Gateway.
 * Le jeu de départ est l'intervention statique insérée par la migration Flyway V2.
 */
@RestController
@RequestMapping("/api/interventions")
public class InterventionController {

    private final InterventionRepository repository;

    public InterventionController(InterventionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public List<InterventionResponse> lister() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public InterventionResponse get(@PathVariable String id) {
        return toResponse(trouver(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public ResponseEntity<InterventionResponse> creer(@Valid @RequestBody InterventionRequest request) {
        Intervention intervention = new Intervention();
        intervention.setId(UUID.randomUUID().toString());
        intervention.setIncidentId(request.incidentId());
        intervention.setTitre(request.titre());
        intervention.setTypeIntervention(request.typeIntervention());
        intervention.setLatitude(request.latitude());
        intervention.setLongitude(request.longitude());
        intervention.setStatut(request.statut() == null ? "EN_ATTENTE" : request.statut());
        Intervention sauvegardee = repository.save(intervention);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(sauvegardee));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public InterventionResponse modifier(@PathVariable String id, @Valid @RequestBody InterventionUpdate request) {
        Intervention intervention = trouver(id);
        if (request.titre() != null) intervention.setTitre(request.titre());
        if (request.statut() != null) intervention.setStatut(request.statut());
        return toResponse(repository.save(intervention));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        Intervention intervention = trouver(id);
        repository.delete(intervention);
        return ResponseEntity.noContent().build();
    }

    private Intervention trouver(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(
                "INTERVENTION_INTROUVABLE", "Intervention introuvable : id=" + id));
    }

    private InterventionResponse toResponse(Intervention i) {
        return new InterventionResponse(i.getId(), i.getIncidentId(), i.getTitre(), i.getTypeIntervention(),
                i.getLatitude(), i.getLongitude(), i.getStatut(), i.getCreatedAt());
    }

    public record InterventionRequest(
            @NotBlank String incidentId,
            @NotBlank String titre,
            @NotBlank String typeIntervention,
            Double latitude,
            Double longitude,
            String statut
    ) {}

    public record InterventionUpdate(String titre, String statut) {}

    public record InterventionResponse(
            String id,
            String incidentId,
            String titre,
            String typeIntervention,
            Double latitude,
            Double longitude,
            String statut,
            Instant createdAt
    ) {}
}
