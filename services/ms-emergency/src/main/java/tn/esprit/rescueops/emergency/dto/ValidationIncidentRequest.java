package tn.esprit.rescueops.emergency.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.rescueops.emergency.domain.NiveauGravite;
import tn.esprit.rescueops.emergency.domain.NiveauPriorite;
import tn.esprit.rescueops.emergency.domain.TypeIncident;

public record ValidationIncidentRequest(

        @NotNull
        TypeIncident type,

        @NotNull
        NiveauGravite gravite,

        @Min(0)
        Integer nombreVictimes,

        @NotNull
        Boolean dangerImmediat,

        @NotNull
        Boolean personnesVulnerables,

        NiveauPriorite prioriteManuelle,

        @Size(max = 500)
        String motifAjustementPriorite

) {
}