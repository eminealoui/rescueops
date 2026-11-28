package tn.esprit.rescueops.emergency.service;

import org.springframework.stereotype.Service;

import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.emergency.domain.StatutIncident;

@Service
public class StatutIncidentService {

    public void verifierTransition(
            StatutIncident actuel,
            StatutIncident suivant
    ) {

        boolean autorisee = switch (actuel) {

            case SIGNALE ->
                    suivant == StatutIncident.VALIDE
                    || suivant == StatutIncident.REJETE
                    || suivant == StatutIncident.FUSIONNE;

            case VALIDE ->
                    suivant == StatutIncident.EN_COURS;

            case EN_COURS ->
                    suivant == StatutIncident.CLOTURE;

            case CLOTURE, REJETE, FUSIONNE -> false;
        };

        if (!autorisee) {

            throw new BusinessRuleException(
                    "Transition interdite de "
                    + actuel + " vers " + suivant
            );
        }
    }
}