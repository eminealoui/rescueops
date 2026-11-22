package tn.esprit.rescueops.user.keycloak;

/** Opérations d'administration Keycloak dont ms-user a besoin (et rien de plus : moindre privilège). */
public interface KeycloakAdminClient {

    /**
     * Ajoute un rôle de realm à un utilisateur. Idempotent : sans effet si le rôle est déjà attribué.
     *
     * @throws KeycloakIndisponibleException si Keycloak ne répond pas ou refuse l'opération
     */
    void attribuerRole(String keycloakId, String role);
}
