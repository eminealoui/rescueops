package tn.esprit.rescueops.dispatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.dispatch.client.ResourceClient;
import tn.esprit.rescueops.dispatch.domain.Affectation;
import tn.esprit.rescueops.dispatch.domain.StatutAffectation;
import tn.esprit.rescueops.dispatch.dto.AffectationRequest;
import tn.esprit.rescueops.dispatch.dto.AffectationResponse;
import tn.esprit.rescueops.dispatch.dto.RecommandationResponse;
import tn.esprit.rescueops.dispatch.repository.AffectationRepository;

import java.util.List;

/**
 * Service métier principal du module Dispatching (ms-dispatch).
 * <p>
 * BF-07 : délégué à {@link ScoringService} pour le calcul des recommandations.<br>
 * BF-08 : {@link #creerAffectation} et {@link #confirmerAffectation}.<br>
 * BF-09 : {@link #annulerEtReproposer} gère les conflits de réservation.
 */
@Service
public class AffectationService {

    private static final Logger log = LoggerFactory.getLogger(AffectationService.class);

    /** Exchange RabbitMQ commun à tous les services RescueOps. */
    private static final String EXCHANGE = "rescueops.events";

    /** Routing key de l'événement publié quand une affectation est confirmée. */
    private static final String RK_AFFECTATION_CONFIRMEE = "affectation.confirmee";

    private final AffectationRepository affectationRepository;
    private final ScoringService scoringService;
    private final ResourceClient resourceClient;
    private final RabbitTemplate rabbitTemplate;

    public AffectationService(AffectationRepository affectationRepository,
                               ScoringService scoringService,
                               ResourceClient resourceClient,
                               RabbitTemplate rabbitTemplate) {
        this.affectationRepository = affectationRepository;
        this.scoringService = scoringService;
        this.resourceClient = resourceClient;
        this.rabbitTemplate = rabbitTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BF-07 : Recommandations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Retourne les recommandations déjà calculées pour un incident (sans recalcul).
     * Pour déclencher un (re)calcul, utiliser {@link ScoringService#calculerRecommandations}.
     */
    @Transactional(readOnly = true)
    public List<RecommandationResponse> getRecommandations(String incidentId) {
        return scoringService.getRecommandations(incidentId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BF-08 : Affectations
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Crée une affectation à l'état PROPOSEE depuis la sélection d'une recommandation.
     * L'opérateur devra ensuite appeler {@link #confirmerAffectation} pour valider.
     *
     * @param request     données de la requête (incidentId, ressourceId)
     * @param operateurId keycloakId de l'opérateur (tiré du JWT, jamais du body)
     * @return la nouvelle affectation à l'état PROPOSEE
     */
    @Transactional
    public AffectationResponse creerAffectation(AffectationRequest request, String operateurId) {
        // Vérifier qu'il n'existe pas déjà une affectation active pour cette ressource
        boolean dejaAffectee = affectationRepository.existsByRessourceIdAndStatutIn(
                request.ressourceId(),
                List.of(StatutAffectation.PROPOSEE, StatutAffectation.CONFIRMEE)
        );
        if (dejaAffectee) {
            throw new ConflictException("RESSOURCE_DEJA_AFFECTEE",
                    "La ressource " + request.ressourceId() + " est déjà affectée à un autre incident.");
        }

        Affectation affectation = new Affectation();
        affectation.setIncidentId(request.incidentId());
        affectation.setRessourceId(request.ressourceId());
        affectation.setTypeRessource(request.typeRessource());
        affectation.setStatut(StatutAffectation.PROPOSEE);
        affectation.setOperateurId(operateurId);

        Affectation saved = affectationRepository.save(affectation);
        log.info("[dispatch] Affectation créée id={} incidentId={} ressourceId={}",
                saved.getId(), saved.getIncidentId(), saved.getRessourceId());
        return toResponse(saved);
    }

    /**
     * L'opérateur confirme une affectation proposée (BF-08).
     * <ol>
     *   <li>Vérifie que l'affectation est bien à l'état PROPOSEE.</li>
     *   <li>Réserve la ressource via ms-resource (OpenFeign).</li>
     *   <li>Passe l'affectation à CONFIRMEE.</li>
     *   <li>Publie l'événement {@code affectation.confirmee} sur RabbitMQ.</li>
     * </ol>
     *
     * @param id          identifiant de l'affectation à confirmer
     * @param operateurId keycloakId de l'opérateur
     */
    @Transactional
    public AffectationResponse confirmerAffectation(Long id, String operateurId) {
        Affectation affectation = getOuEchouer(id);

        if (affectation.getStatut() != StatutAffectation.PROPOSEE) {
            throw new ConflictException("TRANSITION_INVALIDE",
                    "L'affectation " + id + " est déjà " + affectation.getStatut() + ", elle ne peut pas être confirmée.");
        }

        // Réserver la ressource chez ms-resource
        try {
            resourceClient.reserverRessource(affectation.getRessourceId());
        } catch (Exception e) {
            log.error("[dispatch] Échec réservation ressource {} : {}", affectation.getRessourceId(), e.getMessage());
            throw new ConflictException("RESSOURCE_RESERVATION_ECHOUEE",
                    "Impossible de réserver la ressource " + affectation.getRessourceId() + " : " + e.getMessage());
        }

        affectation.setStatut(StatutAffectation.CONFIRMEE);
        affectation.setOperateurId(operateurId);
        Affectation saved = affectationRepository.save(affectation);

        // Publier l'événement affectation.confirmee
        publierAffectationConfirmee(saved);

        log.info("[dispatch] Affectation confirmée id={} par operateurId={}", id, operateurId);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BF-09 : Gestion des conflits
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Annule une affectation et propose automatiquement la meilleure alternative (BF-09).
     * <ol>
     *   <li>Annule l'affectation courante.</li>
     *   <li>Libère la ressource si elle était déjà réservée (CONFIRMEE).</li>
     *   <li>Crée une nouvelle affectation PROPOSEE avec la prochaine meilleure recommandation.</li>
     * </ol>
     *
     * @param id          identifiant de l'affectation à annuler
     * @param operateurId keycloakId de l'opérateur
     * @return nouvelle affectation alternative à l'état PROPOSEE, ou null si aucune alternative
     */
    @Transactional
    public AffectationResponse annulerEtReproposer(Long id, String operateurId) {
        Affectation affectation = getOuEchouer(id);

        if (affectation.getStatut() == StatutAffectation.ANNULEE) {
            throw new ConflictException("DEJA_ANNULEE", "L'affectation " + id + " est déjà annulée.");
        }

        // Libérer la ressource si elle était confirmée (et donc réservée)
        if (affectation.getStatut() == StatutAffectation.CONFIRMEE) {
            try {
                resourceClient.libererRessource(affectation.getRessourceId());
            } catch (Exception e) {
                log.warn("[dispatch] Échec libération ressource {} : {}", affectation.getRessourceId(), e.getMessage());
            }
        }

        // Annuler l'affectation courante
        affectation.setStatut(StatutAffectation.ANNULEE);
        affectation.setOperateurId(operateurId);
        affectationRepository.save(affectation);
        log.info("[dispatch] Affectation annulée id={}", id);

        // Chercher la prochaine meilleure recommandation (différente de la ressource annulée)
        List<RecommandationResponse> recommandations = scoringService.getRecommandations(affectation.getIncidentId());
        RecommandationResponse alternative = recommandations.stream()
                .filter(r -> !r.ressourceId().equals(affectation.getRessourceId()))
                .findFirst()
                .orElse(null);

        if (alternative == null) {
            log.warn("[dispatch] Aucune ressource alternative pour incidentId={}", affectation.getIncidentId());
            return null;
        }

        // Créer la nouvelle affectation alternative
        Affectation nouvelle = new Affectation();
        nouvelle.setIncidentId(affectation.getIncidentId());
        nouvelle.setRessourceId(alternative.ressourceId());
        nouvelle.setTypeRessource(alternative.typeRessource());
        nouvelle.setScore(alternative.score());
        nouvelle.setJustification("[Alternative] " + alternative.justification());
        nouvelle.setStatut(StatutAffectation.PROPOSEE);
        nouvelle.setOperateurId(operateurId);
        Affectation savedAlternative = affectationRepository.save(nouvelle);
        log.info("[dispatch] Affectation alternative proposée id={} ressourceId={}",
                savedAlternative.getId(), savedAlternative.getRessourceId());
        return toResponse(savedAlternative);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listes
    // ─────────────────────────────────────────────────────────────────────────

    /** Toutes les affectations, avec filtre optionnel par statut. */
    @Transactional(readOnly = true)
    public List<AffectationResponse> listerAffectations(StatutAffectation statut) {
        List<Affectation> liste = (statut != null)
                ? affectationRepository.findByStatut(statut)
                : affectationRepository.findAll();
        return liste.stream().map(this::toResponse).toList();
    }

    /** Toutes les affectations pour un incident donné. */
    @Transactional(readOnly = true)
    public List<AffectationResponse> listerParIncident(String incidentId) {
        return affectationRepository.findByIncidentId(incidentId)
                .stream().map(this::toResponse).toList();
    }

    /** Récupère une affectation par son ID ou lève NotFoundException. */
    @Transactional(readOnly = true)
    public AffectationResponse getAffectation(Long id) {
        return toResponse(getOuEchouer(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Méthodes privées
    // ─────────────────────────────────────────────────────────────────────────

    private Affectation getOuEchouer(Long id) {
        return affectationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("AFFECTATION_INTROUVABLE",
                        "Affectation introuvable : id=" + id));
    }

    /** Publie l'événement affectation.confirmee sur l'exchange RabbitMQ rescueops.events. */
    private void publierAffectationConfirmee(Affectation affectation) {
        AffectationConfirmeePayload payload = new AffectationConfirmeePayload(
                affectation.getId().toString(),
                affectation.getIncidentId(),
                affectation.getRessourceId(),
                affectation.getTypeRessource(),
                affectation.getOperateurId()
        );
        RescueOpsEvent<AffectationConfirmeePayload> event =
                RescueOpsEvent.of(RK_AFFECTATION_CONFIRMEE, payload);
        rabbitTemplate.convertAndSend(EXCHANGE, RK_AFFECTATION_CONFIRMEE, event);
        log.info("[dispatch] Événement {} publié pour affectationId={}", RK_AFFECTATION_CONFIRMEE, affectation.getId());
    }

    /** Convertit une entité Affectation en DTO public (jamais exposer l'entité JPA). */
    private AffectationResponse toResponse(Affectation a) {
        return new AffectationResponse(
                a.getId(),
                a.getIncidentId(),
                a.getRessourceId(),
                a.getTypeRessource(),
                a.getStatut(),
                a.getScore(),
                a.getJustification(),
                a.getOperateurId(),
                a.getCreatedAt(),
                a.getUpdatedAt()
        );
    }

    /**
     * Payload de l'événement affectation.confirmee, consommé par ms-intervention et ms-emergency.
     */
    public record AffectationConfirmeePayload(
            String affectationId,
            String incidentId,
            String ressourceId,
            String typeRessource,
            String operateurId
    ) {}
}
