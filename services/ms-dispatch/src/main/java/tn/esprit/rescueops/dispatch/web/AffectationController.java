package tn.esprit.rescueops.dispatch.web;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.dispatch.domain.StatutAffectation;
import tn.esprit.rescueops.dispatch.dto.AffectationRequest;
import tn.esprit.rescueops.dispatch.dto.AffectationResponse;
import tn.esprit.rescueops.dispatch.dto.RecommandationRequest;
import tn.esprit.rescueops.dispatch.dto.RecommandationResponse;
import tn.esprit.rescueops.dispatch.service.AffectationService;
import tn.esprit.rescueops.dispatch.service.ScoringService;

import java.util.List;

/**
 * Point d'entrée REST du module Dispatching Intelligent (ms-dispatch).
 * <p>
 * Préfixe : {@code /api/dispatch}  — NE PAS CHANGER (contrat d'équipe + Gateway).
 * <p>
 * Endpoints :
 * <ul>
 *   <li>BF-07 : {@code GET  /recommandations/{incidentId}} — liste triée par score</li>
 *   <li>BF-07 : {@code POST /recommandations/calculer}    — déclenche le (re)calcul</li>
 *   <li>BF-08 : {@code POST /affectations}               — créer (PROPOSEE)</li>
 *   <li>BF-08 : {@code GET  /affectations}               — lister (filtre statut optionnel)</li>
 *   <li>BF-08 : {@code GET  /affectations/{id}}          — détail</li>
 *   <li>BF-08 : {@code PATCH /affectations/{id}/confirmer} — valider</li>
 *   <li>BF-09 : {@code PATCH /affectations/{id}/annuler}   — annuler + reproposer</li>
 *   <li>BF-07 : {@code GET  /affectations/incident/{incidentId}} — par incident</li>
 * </ul>
 * <p>
 * Rôles autorisés : {@code OPERATEUR} et {@code ADMIN} (définis dans rescueops-common Role).
 * L'identité de l'opérateur est toujours tirée du JWT via {@link CurrentUser}, jamais du body.
 */
@RestController
@RequestMapping("/api/dispatch")
public class AffectationController {

    private final AffectationService affectationService;
    private final ScoringService scoringService;

    public AffectationController(AffectationService affectationService, ScoringService scoringService) {
        this.affectationService = affectationService;
        this.scoringService = scoringService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BF-07 : Recommandations (scoring multicritère)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne les recommandations déjà calculées pour un incident, triées par score décroissant.
     * <p>
     * L'opérateur consulte cette liste sur l'écran "Recommandations classées"
     * avant de valider une affectation.
     *
     * @param incidentId identifiant de l'incident
     * @return liste de recommandations classées (vide si aucun calcul effectué)
     */
    @GetMapping("/recommandations/{incidentId}")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public List<RecommandationResponse> getRecommandations(@PathVariable String incidentId) {
        return affectationService.getRecommandations(incidentId);
    }

    /**
     * Déclenche manuellement le calcul (ou recalcul) du scoring pour un incident.
     * <p>
     * Utile quand l'opérateur veut actualiser les recommandations suite à un changement
     * de disponibilité des ressources.
     *
     * @param request corps contenant incidentId, coordonnées GPS, type de ressource
     * @return liste de recommandations fraîchement calculées
     */
    @PostMapping("/recommandations/calculer")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public List<RecommandationResponse> calculerRecommandations(
            @Valid @RequestBody RecommandationRequest request) {
        return scoringService.calculerRecommandations(
                request.incidentId(),
                request.latitudeIncident(),
                request.longitudeIncident(),
                request.typeRessource()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BF-08 : Affectations (CRUD + cycle de vie)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée une affectation à l'état PROPOSEE (l'opérateur a sélectionné une recommandation).
     * L'affectation devra être confirmée séparément via PATCH .../confirmer.
     *
     * @param request corps de la requête (incidentId, ressourceId)
     * @return 201 Created + affectation PROPOSEE
     */
    @PostMapping("/affectations")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public ResponseEntity<AffectationResponse> creerAffectation(
            @Valid @RequestBody AffectationRequest request) {
        String operateurId = CurrentUser.current()
                .map(CurrentUser::keycloakId)
                .orElse("system");
        AffectationResponse response = affectationService.creerAffectation(request, operateurId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Liste toutes les affectations, avec filtre optionnel sur le statut.
     *
     * @param statut filtre optionnel : PROPOSEE, CONFIRMEE, ANNULEE
     * @return liste des affectations
     */
    @GetMapping("/affectations")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public List<AffectationResponse> listerAffectations(
            @RequestParam(required = false) StatutAffectation statut) {
        return affectationService.listerAffectations(statut);
    }

    /**
     * Retourne le détail d'une affectation par son ID.
     *
     * @param id identifiant de l'affectation
     * @return 200 + affectation, ou 404 si introuvable
     */
    @GetMapping("/affectations/{id}")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public AffectationResponse getAffectation(@PathVariable Long id) {
        return affectationService.getAffectation(id);
    }

    /**
     * Retourne toutes les affectations liées à un incident (utile pour l'historique).
     *
     * @param incidentId identifiant de l'incident
     * @return liste des affectations pour cet incident
     */
    @GetMapping("/affectations/incident/{incidentId}")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public List<AffectationResponse> listerParIncident(@PathVariable String incidentId) {
        return affectationService.listerParIncident(incidentId);
    }

    /**
     * L'opérateur confirme une affectation proposée (BF-08).
     * Réserve la ressource chez ms-resource et publie l'événement affectation.confirmee.
     *
     * @param id identifiant de l'affectation à confirmer
     * @return 200 + affectation CONFIRMEE, ou 409 si déjà traitée, ou 404 si introuvable
     */
    @PatchMapping("/affectations/{id}/confirmer")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public AffectationResponse confirmerAffectation(@PathVariable Long id) {
        String operateurId = CurrentUser.current()
                .map(CurrentUser::keycloakId)
                .orElse("system");
        return affectationService.confirmerAffectation(id, operateurId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BF-09 : Gestion des conflits / Annulation + reproposition
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Annule une affectation et propose automatiquement la meilleure ressource alternative (BF-09).
     * <ul>
     *   <li>Passe l'affectation courante à ANNULEE.</li>
     *   <li>Libère la ressource chez ms-resource si elle était déjà réservée.</li>
     *   <li>Crée et retourne une nouvelle affectation PROPOSEE avec la ressource alternative.</li>
     * </ul>
     *
     * @param id identifiant de l'affectation à annuler
     * @return 200 + nouvelle affectation alternative PROPOSEE (null si aucune alternative disponible)
     */
    @PatchMapping("/affectations/{id}/annuler")
    @PreAuthorize("hasRole('OPERATEUR') or hasRole('ADMIN')")
    public ResponseEntity<AffectationResponse> annulerEtReproposer(@PathVariable Long id) {
        String operateurId = CurrentUser.current()
                .map(CurrentUser::keycloakId)
                .orElse("system");
        AffectationResponse alternative = affectationService.annulerEtReproposer(id, operateurId);
        if (alternative == null) {
            return ResponseEntity.ok().build(); // 200 sans corps : aucune alternative
        }
        return ResponseEntity.ok(alternative);
    }
}
