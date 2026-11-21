package tn.esprit.rescueops.common.security;

/**
 * Les 8 rôles de sécurité RescueOps.
 * <p>
 * Contrat d'équipe : ces noms sont identiques dans le realm Keycloak, les {@code @PreAuthorize}
 * des services et les guards Angular. Keycloak est la seule autorité qui attribue ces rôles.
 */
public enum Role {
    CITOYEN,
    OPERATEUR,
    GESTIONNAIRE_RESSOURCES,
    AMBULANCIER,
    COORDINATEUR_HOSPITALIER,
    FORMATEUR,
    APPRENANT,
    ADMIN;

    /** Autorité Spring Security correspondante, par exemple {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }

    /** Retourne le rôle correspondant au nom, ou {@code null} s'il n'est pas un rôle RescueOps. */
    static Role fromName(String name) {
        for (Role r : values()) {
            if (r.name().equals(name)) {
                return r;
            }
        }
        return null;
    }
}
