package tn.esprit.rescueops.emergency;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ms-emergency - Module 1 : Gestion des urgences.
 * <p>
 * Port 8081. Sécurité, erreurs et événements sont fournis automatiquement par rescueops-common.
 */
@SpringBootApplication
@EnableFeignClients
public class EmergencyServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmergencyServiceApplication.class, args);
    }
}
