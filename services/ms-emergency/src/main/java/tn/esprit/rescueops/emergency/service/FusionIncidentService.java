package tn.esprit.rescueops.emergency.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.emergency.domain.Incident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.domain.TypeActionIncident;
import tn.esprit.rescueops.emergency.dto.FusionIncidentRequest;
import tn.esprit.rescueops.emergency.dto.FusionIncidentResponse;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;


@Service
public class FusionIncidentService {

    private final IncidentRepository repository;

    private final StatutIncidentService statutService;

    private final HistoriqueIncidentService historiqueService;

    public FusionIncidentService(
            IncidentRepository repository,
            StatutIncidentService statutService,
            HistoriqueIncidentService historiqueService
    ) {

        this.repository = repository;
        this.statutService = statutService;
        this.historiqueService = historiqueService;
    }

    @Transactional
    public FusionIncidentResponse fusionner(
            UUID principalId,
            FusionIncidentRequest request
    ) {

        UUID secondaireId =
                request.incidentSecondaireId();

        // 1. Interdire la fusion avec soi-meme

        if (principalId.equals(secondaireId)) {

            throw new BusinessRuleException(
                    "Un incident ne peut pas etre fusionne avec lui-meme"
            );
        }

        // 2. Verrouiller dans un ordre stable

        boolean principalPremier =
                principalId.compareTo(secondaireId) < 0;

        Incident premier = chercher(
                principalPremier ? principalId : secondaireId
        );

        Incident second = chercher(
                principalPremier ? secondaireId : principalId
        );

        Incident principal =
                principalPremier ? premier : second;

        Incident secondaire =
                principalPremier ? second : premier;

        // 3. Verifier le statut principal

        if (!estActif(principal.getStatut())) {

            throw new BusinessRuleException(
                    "Incident principal non eligible a la fusion"
            );
        }

        // 4. Verifier le statut secondaire

        if (secondaire.getStatut()
                != StatutIncident.SIGNALE) {

            throw new BusinessRuleException(
                    "Seul un incident signale peut etre fusionne"
            );
        }

        // 5. Interdire les fusions multiples

        if (principal.getIncidentPrincipalId() != null
                || secondaire.getIncidentPrincipalId() != null) {

            throw new BusinessRuleException(
                    "Un incident est deja rattache"
            );
        }

        // 6. Interdire les fusions en cascade

        if (repository.existsByIncidentPrincipalId(
                secondaire.getId()
        )) {

            throw new BusinessRuleException(
                    "L'incident secondaire possede deja des signalements rattaches"
            );
        }

        // 7. Verifier la transition

        statutService.verifierTransition(
                secondaire.getStatut(),
                StatutIncident.FUSIONNE
        );

        // 8. Enregistrer les informations

        secondaire.setIncidentPrincipalId(
                principal.getId()
        );

        secondaire.setStatut(
                StatutIncident.FUSIONNE
        );

        secondaire.setMotifFusion(
                request.motif().trim()
        );

        secondaire.setFusionnePar(
                CurrentUser.current()
                        .orElseThrow()
                        .keycloakId()
        );

        secondaire.setFusionneLe(
                Instant.now()
        );

        repository.save(secondaire);

        historiqueService.enregistrer(

        secondaire.getId(),
        TypeActionIncident.FUSION,
        StatutIncident.SIGNALE,
        StatutIncident.FUSIONNE,
        secondaire.getFusionnePar(),

        "Fusion vers l'incident : "
        + principal.getId()
        + " | Motif : "
        + secondaire.getMotifFusion()
        );

        historiqueService.enregistrer(

                principal.getId(),
                TypeActionIncident.RATTACHEMENT_DOUBLON,
                principal.getStatut(),
                principal.getStatut(),
                secondaire.getFusionnePar(),

                "Rattachement de l'incident : "
                + secondaire.getId()
                + " | Motif : "
                + secondaire.getMotifFusion()
        );

        // 9. Retourner le resultat

        return new FusionIncidentResponse(

                principal.getId(),
                secondaire.getId(),

                principal.getStatut(),
                secondaire.getStatut(),

                secondaire.getMotifFusion(),
                secondaire.getFusionneLe()
        );
    }

    private Incident chercher(UUID id) {

        return repository.trouverPourModification(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Incident introuvable"
                        )
                );
    }

    private boolean estActif(StatutIncident statut) {

        return statut == StatutIncident.SIGNALE
                || statut == StatutIncident.VALIDE
                || statut == StatutIncident.EN_COURS;
    }
}