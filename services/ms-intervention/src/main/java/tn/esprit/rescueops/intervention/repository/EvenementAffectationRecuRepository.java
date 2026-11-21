
package tn.esprit.rescueops.intervention.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.esprit.rescueops.intervention.domain.EvenementAffectationRecu;

import java.util.UUID;

public interface EvenementAffectationRecuRepository
    extends JpaRepository<EvenementAffectationRecu, UUID> {

}
