package tn.esprit.rescueops.hospital.dto;

import java.util.List;
import java.util.UUID;

public record HopitalResponse(UUID id, String nom, String adresse, String ville, double latitude, double longitude,
                              String telephone, boolean actif, List<ServiceResponse> services) {
}
