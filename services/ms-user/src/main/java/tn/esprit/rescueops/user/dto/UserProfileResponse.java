package tn.esprit.rescueops.user.dto;

import java.time.Instant;

/** Profil renvoyé par l'API. L'identifiant public est le keycloakId ; l'id interne n'est jamais exposé. */
public record UserProfileResponse(String keycloakId, String email, String firstName, String lastName, String phone,
                                  String address, String organisation, boolean active, Instant createdAt,
                                  Instant updatedAt) {
}
