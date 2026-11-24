package tn.esprit.rescueops.dispatch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.dispatch.client.ResourceClient;
import tn.esprit.rescueops.dispatch.domain.Recommandation;
import tn.esprit.rescueops.dispatch.dto.RecommandationResponse;
import tn.esprit.rescueops.dispatch.repository.RecommandationRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Algorithme de scoring multicritère (BF-07).
 * <p>
 * Pour un incident donné, ce service récupère les ressources disponibles via ms-resource (OpenFeign),
 * calcule un score pour chacune, persiste les recommandations en base et retourne la liste triée.
 * <p>
 * <b>Formule de scoring :</b>
 * <pre>
 *   Score = (0.40 × ScoreDistance) + (0.40 × ScoreDisponibilité) + (0.20 × ScoreCompétences)
 *
 *   ScoreDistance      = 1 / (1 + distanceKm)         normalisé [0, 1]
 *   ScoreDisponibilité = 1.0 si disponible, 0.0 sinon
 *   ScoreCompétences   = 1.0 si compétences OK, 0.0 sinon
 * </pre>
 */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    // Poids de chaque critère (somme = 1.0)
    private static final double POIDS_DISTANCE      = 0.40;
    private static final double POIDS_DISPONIBILITE = 0.40;
    private static final double POIDS_COMPETENCES   = 0.20;

    private final ResourceClient resourceClient;
    private final RecommandationRepository recommandationRepository;

    public ScoringService(ResourceClient resourceClient,
                          RecommandationRepository recommandationRepository) {
        this.resourceClient = resourceClient;
        this.recommandationRepository = recommandationRepository;
    }

    /**
     * Calcule et persiste les recommandations pour un incident.
     * Les anciennes recommandations de l'incident sont supprimées avant le recalcul.
     *
     * @param incidentId        identifiant de l'incident
     * @param latIncident       latitude GPS de l'incident
     * @param lonIncident       longitude GPS de l'incident
     * @param typeRessource     filtre optionnel sur le type de ressource
     * @return liste de recommandations triées par score décroissant
     */
    @Transactional
    public List<RecommandationResponse> calculerRecommandations(
            String incidentId, Double latIncident, Double lonIncident, String typeRessource) {

        log.info("[scoring] Calcul des recommandations pour incidentId={} lat={} lon={}",
                incidentId, latIncident, lonIncident);

        // 1. Supprimer les anciennes recommandations pour ce même incident (recalcul propre)
        recommandationRepository.deleteByIncidentId(incidentId);

        // 2. Récupérer les ressources disponibles via ms-resource (OpenFeign)
        List<ResourceClient.ResourceDto> ressources;
        try {
            ressources = resourceClient.getAvailableResources(typeRessource);
        } catch (Exception e) {
            log.warn("[scoring] ms-resource indisponible, fallback sur liste vide : {}", e.getMessage());
            ressources = new ArrayList<>();
        }

        if (ressources.isEmpty()) {
            log.warn("[scoring] Aucune ressource disponible pour incidentId={}", incidentId);
            return List.of();
        }

        // 3. Calculer le score de chaque ressource
        List<Recommandation> recommandations = ressources.stream()
                .map(r -> calculerScore(incidentId, latIncident, lonIncident, r))
                .sorted(Comparator.comparingDouble(Recommandation::getScore).reversed())
                .toList();

        // 4. Persister
        List<Recommandation> sauvegardees = recommandationRepository.saveAll(recommandations);
        log.info("[scoring] {} recommandations sauvegardées pour incidentId={}", sauvegardees.size(), incidentId);

        // 5. Retourner les DTOs
        return sauvegardees.stream().map(this::toResponse).toList();
    }

    /**
     * Retourne les recommandations déjà calculées pour un incident (sans recalcul).
     */
    @Transactional(readOnly = true)
    public List<RecommandationResponse> getRecommandations(String incidentId) {
        return recommandationRepository.findByIncidentIdOrderByScoreDesc(incidentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Méthodes privées
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Calcule le score multicritère d'une ressource par rapport à un incident.
     */
    private Recommandation calculerScore(String incidentId,
                                         Double latIncident, Double lonIncident,
                                         ResourceClient.ResourceDto ressource) {
        // --- Critère 1 : Distance ---
        double distanceKm = calculerDistance(latIncident, lonIncident,
                ressource.latitude(), ressource.longitude());
        double scoreDistance = 1.0 / (1.0 + distanceKm);

        // --- Critère 2 : Disponibilité ---
        boolean disponible = Boolean.TRUE.equals(ressource.disponible());
        double scoreDisponibilite = disponible ? 1.0 : 0.0;

        // --- Critère 3 : Compétences ---
        boolean competencesOk = !ressource.competences().isEmpty();
        double scoreCompetences = competencesOk ? 1.0 : 0.0;

        // --- Score final pondéré ---
        double scoreGlobal = (POIDS_DISTANCE      * scoreDistance)
                           + (POIDS_DISPONIBILITE * scoreDisponibilite)
                           + (POIDS_COMPETENCES   * scoreCompetences);

        // --- Justification lisible ---
        String justification = String.format(
                "Score %.2f | Distance %.1f km (×%.0f%%) | Disponible: %s (×%.0f%%) | Compétences: %s (×%.0f%%)",
                scoreGlobal,
                distanceKm, POIDS_DISTANCE * 100,
                disponible ? "✓" : "✗", POIDS_DISPONIBILITE * 100,
                competencesOk ? "✓" : "✗", POIDS_COMPETENCES * 100
        );

        // --- Construire l'entité ---
        Recommandation rec = new Recommandation();
        rec.setIncidentId(incidentId);
        rec.setRessourceId(ressource.id());
        rec.setNomRessource(ressource.nom());
        rec.setTypeRessource(ressource.type());
        rec.setScore(Math.round(scoreGlobal * 10000.0) / 10000.0); // 4 décimales
        rec.setJustification(justification);
        rec.setDistanceKm(Math.round(distanceKm * 10.0) / 10.0); // 1 décimale
        rec.setLatitudeIncident(latIncident);
        rec.setLongitudeIncident(lonIncident);
        rec.setDisponible(disponible);
        rec.setCompetencesOk(competencesOk);
        return rec;
    }

    /**
     * Calcule la distance en km entre deux points GPS via la formule de Haversine.
     * Retourne 0.0 si l'une des coordonnées est null.
     */
    private double calculerDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            return 0.0; // distance inconnue → score maximal par défaut
        }
        final int RAYON_TERRE_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAYON_TERRE_KM * c;
    }

    /** Convertit une entité Recommandation en DTO public. */
    private RecommandationResponse toResponse(Recommandation r) {
        return new RecommandationResponse(
                r.getId(),
                r.getIncidentId(),
                r.getRessourceId(),
                r.getNomRessource(),
                r.getTypeRessource(),
                r.getScore(),
                r.getJustification(),
                r.getDistanceKm(),
                r.getDisponible(),
                r.getCompetencesOk(),
                r.getCreatedAt()
        );
    }
}
