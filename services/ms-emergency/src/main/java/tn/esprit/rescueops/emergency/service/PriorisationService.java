package tn.esprit.rescueops.emergency.service;

import org.springframework.stereotype.Service;

import tn.esprit.rescueops.emergency.domain.NiveauGravite;
import tn.esprit.rescueops.emergency.domain.NiveauPriorite;
import tn.esprit.rescueops.emergency.domain.TypeIncident;
import tn.esprit.rescueops.emergency.dto.ResultatPriorisation;

@Service
public class PriorisationService {

    public ResultatPriorisation calculer(
            TypeIncident type,
            NiveauGravite gravite,
            Integer nombreVictimes,
            boolean dangerImmediat,
            boolean personnesVulnerables
    ) {

        int scoreType = switch (type) {

            case INCENDIE,
                 URGENCE_MEDICALE,
                 CATASTROPHE_NATURELLE -> 20;

            case ACCIDENT_ROUTIER,
                 AGRESSION,
                 INTOXICATION -> 15;

            case AUTRE -> 5;
        };

        int scoreGravite = switch (gravite) {

            case MINEURE -> 5;

            case MODEREE -> 10;

            case GRAVE -> 20;

            case TRES_GRAVE -> 30;
        };

        int scoreVictimes = nombreVictimes == null
                ? 0
                : Math.min(Math.max(nombreVictimes, 0), 4) * 5;

        int scoreDanger = dangerImmediat ? 20 : 0;

        int scoreVulnerabilite =
                personnesVulnerables ? 10 : 0;

        int scoreTotal =
                scoreType
                + scoreGravite
                + scoreVictimes
                + scoreDanger
                + scoreVulnerabilite;

        NiveauPriorite priorite;

        if (scoreTotal >= 75) {

            priorite = NiveauPriorite.CRITIQUE;

        } else if (scoreTotal >= 50) {

            priorite = NiveauPriorite.HAUTE;

        } else if (scoreTotal >= 25) {

            priorite = NiveauPriorite.MOYENNE;

        } else {

            priorite = NiveauPriorite.FAIBLE;
        }

        String explication =
                "Type: +" + scoreType
                + ", gravite: +" + scoreGravite
                + ", victimes: +" + scoreVictimes
                + ", danger: +" + scoreDanger
                + ", vulnerabilite: +" + scoreVulnerabilite;

        if (nombreVictimes == null) {
            explication += " (nombre de victimes inconnu)";
        }

        return new ResultatPriorisation(
                scoreTotal,
                priorite,
                explication
        );
    }
}