package tn.esprit.rescueops.intervention;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ms-intervention - Module 4 : Gestion des interventions.
 * <p>
 * Port 8084. Sécurité, erreurs et événements sont fournis automatiquement par rescueops-common.
 */
@SpringBootApplication
@EnableFeignClients
public class InterventionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterventionServiceApplication.class, args);
    }
}
