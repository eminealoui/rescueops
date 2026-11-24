package tn.esprit.rescueops.dispatch.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.rescueops.dispatch.domain.Affectation;
import tn.esprit.rescueops.dispatch.domain.StatutAffectation;

import java.util.List;

/**
 * Accès aux données des affectations (dispatch_db.affectation).
 * Contrat : ne jamais accéder aux tables d'un autre service.
 */
public interface AffectationRepository extends JpaRepository<Affectation, Long> {

    /** Toutes les affectations pour un incident donné. */
    List<Affectation> findByIncidentId(String incidentId);

    /** Affectations par statut (PROPOSEE / CONFIRMEE / ANNULEE). */
    List<Affectation> findByStatut(StatutAffectation statut);

    /** Vérifier si une ressource a déjà une affectation active (PROPOSEE ou CONFIRMEE). */
    boolean existsByRessourceIdAndStatutIn(String ressourceId, List<StatutAffectation> statuts);
}
