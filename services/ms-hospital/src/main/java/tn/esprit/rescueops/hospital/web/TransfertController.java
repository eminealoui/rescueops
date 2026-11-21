package tn.esprit.rescueops.hospital.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.dto.CreerTransfertRequest;
import tn.esprit.rescueops.hospital.dto.TransfertResponse;
import tn.esprit.rescueops.hospital.service.FluxService;
import tn.esprit.rescueops.hospital.service.TransfertService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Demandes de transfert (BF-15). Les règles de transition sont dans TransfertService. */
@RestController
@RequestMapping("/api/transfers")
public class TransfertController {

    static final String TERRAIN = "hasAnyRole('AMBULANCIER', 'OPERATEUR', 'ADMIN')";
    static final String RESERVATION = "hasAnyRole('AMBULANCIER', 'OPERATEUR', 'COORDINATEUR_HOSPITALIER', 'ADMIN')";
    static final String HOPITAL = "hasAnyRole('COORDINATEUR_HOSPITALIER', 'ADMIN')";
    static final String OPERATIONNEL = "hasAnyRole('OPERATEUR', 'AMBULANCIER', 'COORDINATEUR_HOSPITALIER', 'GESTIONNAIRE_RESSOURCES', 'ADMIN')";

    public record ReserverRequest(@NotNull UUID serviceId) {
    }

    public record RefuserRequest(@Size(max = 500) String motif) {
    }

    private final TransfertService service;
    private final FluxService flux;

    public TransfertController(TransfertService service, FluxService flux) {
        this.service = service;
        this.flux = flux;
    }

    /** Ticker « EN DIRECT » : réservé aux rôles opérationnels, sans données personnelles. */
    @GetMapping("/feed")
    @PreAuthorize(OPERATIONNEL)
    public List<FluxService.Evenement> feed(@RequestParam(required = false) Instant since) {
        return flux.derniers(since);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(TERRAIN)
    public TransfertResponse creer(@Valid @RequestBody CreerTransfertRequest req, JwtAuthenticationToken auth) {
        return service.creer(req, user(auth));
    }

    @GetMapping
    @PreAuthorize(OPERATIONNEL)
    public List<TransfertResponse> lister(@RequestParam(required = false) StatutTransfert statut,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "20") int size) {
        return service.lister(statut, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(OPERATIONNEL)
    public TransfertResponse trouver(@PathVariable UUID id) {
        return service.trouver(id);
    }

    @PostMapping("/{id}/reserve")
    @PreAuthorize(RESERVATION)
    public TransfertResponse reserver(@PathVariable UUID id, @Valid @RequestBody ReserverRequest req, JwtAuthenticationToken auth) {
        return service.reserver(id, req.serviceId(), user(auth));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize(HOPITAL)
    public TransfertResponse confirmer(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return service.confirmer(id, user(auth));
    }

    @PostMapping("/{id}/refuse")
    @PreAuthorize(HOPITAL)
    public TransfertResponse refuser(@PathVariable UUID id, @Valid @RequestBody(required = false) RefuserRequest req,
                                     JwtAuthenticationToken auth) {
        return service.refuser(id, req == null ? null : req.motif(), user(auth));
    }

    @PostMapping("/{id}/admit")
    @PreAuthorize(HOPITAL)
    public TransfertResponse admettre(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return service.admettre(id, user(auth));
    }

    @PostMapping("/{id}/discharge")
    @PreAuthorize(HOPITAL)
    public TransfertResponse sortie(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return service.sortie(id, user(auth));
    }

    /** Le service vérifie en plus que l'appelant est le demandeur, un coordinateur ou un admin. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize(RESERVATION)
    public TransfertResponse annuler(@PathVariable UUID id, JwtAuthenticationToken auth) {
        return service.annuler(id, user(auth));
    }

    private static CurrentUser user(JwtAuthenticationToken auth) {
        return CurrentUser.from(auth.getToken());
    }
}
