package tn.esprit.rescueops.user.dto;

/**
 * Vue minimale d'un utilisateur, utilisée par les autres services et écrans pour afficher un nom
 * à la place d'un identifiant (ex. « Meriem Ben Salah » au lieu d'un sub Keycloak).
 */
public record UserSummaryDto(String keycloakId, String displayName, String organisation) {
}
