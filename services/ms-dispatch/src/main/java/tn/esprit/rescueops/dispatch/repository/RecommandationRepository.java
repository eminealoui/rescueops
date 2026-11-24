package tn.esprit.rescueops.dispatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.dispatch.domain.Recommandation;

import java.util.List;

/**
 * Accès aux données des recommandations de scoring (dispatch_db.recommandation).
 */
public interface RecommandationRepository extends JpaRepository<Recommandation, Long> {

    /** Recommandations pour un incident, triées par score décroissant (meilleure en premier). */
    List<Recommandation> findByIncidentIdOrderByScoreDesc(String incidentId);

    /** Supprimer les anciennes recommandations d'un incident avant de recalculer. */
    void deleteByIncidentId(String incidentId);
}
