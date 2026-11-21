package tn.esprit.rescueops.training;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ms-training - Module 6 : Formations, certifications et simulations.
 * <p>
 * Port 8086. Sécurité, erreurs et événements sont fournis automatiquement par rescueops-common.
 */
@SpringBootApplication
@EnableFeignClients
public class TrainingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingServiceApplication.class, args);
    }
}
