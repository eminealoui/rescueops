package tn.esprit.rescueops.emergency.dto;

import tn.esprit.rescueops.emergency.domain.NiveauPriorite;

public record ResultatPriorisation(

        int score,
        NiveauPriorite priorite,
        String explication

) {
}