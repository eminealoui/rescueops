package tn.esprit.rescueops.hospital.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rescueops.hospital.dto.*;
import tn.esprit.rescueops.hospital.service.EtablissementService;

import java.util.List;
import java.util.UUID;

/** Établissements et services. Lecture : tout utilisateur connecté. Écriture : coordinateur hospitalier ou admin. */
@RestController
@RequestMapping("/api/hospitals")
public class HopitalController {

    static final String ECRITURE = "hasAnyRole('COORDINATEUR_HOSPITALIER', 'ADMIN')";

    private final EtablissementService service;

    public HopitalController(EtablissementService service) {
        this.service = service;
    }

    @GetMapping
    public List<HopitalResponse> lister() {
        return service.listerHopitaux();
    }

    @GetMapping("/{id}")
    public HopitalResponse trouver(@PathVariable UUID id) {
        return service.trouverHopital(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ECRITURE)
    public HopitalResponse creer(@Valid @RequestBody HopitalRequest req) {
        return service.creerHopital(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize(ECRITURE)
    public HopitalResponse modifier(@PathVariable UUID id, @Valid @RequestBody HopitalRequest req) {
        return service.modifierHopital(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(ECRITURE)
    public void supprimer(@PathVariable UUID id) {
        service.supprimerHopital(id);
    }

    @PostMapping("/{id}/services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(ECRITURE)
    public ServiceResponse ajouterService(@PathVariable UUID id, @Valid @RequestBody ServiceRequest req) {
        return service.ajouterService(id, req);
    }

    @PutMapping("/services/{serviceId}")
    @PreAuthorize(ECRITURE)
    public ServiceResponse changerEtat(@PathVariable UUID serviceId, @Valid @RequestBody ServiceActifRequest req) {
        return service.changerEtatService(serviceId, req.actif());
    }

    @PatchMapping("/services/{serviceId}/capacity")
    @PreAuthorize(ECRITURE)
    public ServiceResponse ajusterCapacite(@PathVariable UUID serviceId, @Valid @RequestBody CapaciteRequest req,
                                           JwtAuthenticationToken auth) {
        return service.ajusterCapacite(serviceId, req, auth.getToken().getSubject());
    }
}
