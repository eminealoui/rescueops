package tn.esprit.rescueops.training.repository;

import tn.esprit.rescueops.training.model.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {
}
