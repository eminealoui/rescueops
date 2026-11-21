package tn.esprit.rescueops.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Champs qu'un utilisateur peut modifier sur SON profil. Tout autre champ envoyé (keycloakId, email, active,
 * roles…) est ignoré : l'identité et le statut ne se modifient jamais par cette route.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record UpdateMyProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Pattern(regexp = "^$|^\\+?[1-9][0-9]{7,14}$", message = "format international attendu, ex. +21698123456 (vide pour effacer)")
        String phone,
        @Size(max = 255) String address) {
}
