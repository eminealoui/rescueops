package tn.esprit.rescueops.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tn.esprit.rescueops.common.security.Role;

/** Demande d'un rôle professionnel (jamais ADMIN, jamais CITOYEN qui est déjà attribué à l'inscription). */
public record DemandeRoleRequest(@NotNull Role role,
                                 @NotBlank @Size(max = 150) String organisation,
                                 @Size(max = 500) String motif) {
}
