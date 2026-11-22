
package tn.esprit.rescueops.intervention.dto;

import jakarta.validation.constraints.*;
import tn.esprit.rescueops.intervention.domain.TypeItineraire;

import java.math.BigDecimal;

public record ItineraireRequest(

    @NotNull
    TypeItineraire typeItineraire,

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    BigDecimal latitudeDepart,

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    BigDecimal longitudeDepart,

    @NotNull
    @DecimalMin("-90")
    @DecimalMax("90")
    BigDecimal latitudeDestination,

    @NotNull
    @DecimalMin("-180")
    @DecimalMax("180")
    BigDecimal longitudeDestination

) {
}
