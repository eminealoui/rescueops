package tn.esprit.rescueops.emergency.event;

import java.util.UUID;

public record IncidentPrioriseEvent(
        UUID incidentId
) {
}