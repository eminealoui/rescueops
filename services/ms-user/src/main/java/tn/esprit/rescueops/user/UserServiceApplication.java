package tn.esprit.rescueops.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ms-user - service transversal des profils métier (port 8087), partagé par toute l'équipe.
 * Keycloak gère comptes, mots de passe et rôles ; ce service gère uniquement les profils.
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
