package tn.esprit.rescueops.resource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ms-resource - Module 2 : Gestion des ressources.
 * <p>
 * Port 8082. Sécurité, erreurs et événements sont fournis automatiquement par rescueops-common.
 */
@SpringBootApplication
@EnableFeignClients
public class ResourceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ResourceServiceApplication.class, args);
    }
}
