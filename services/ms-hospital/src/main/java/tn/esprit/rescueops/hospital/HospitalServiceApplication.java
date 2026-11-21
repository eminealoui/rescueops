package tn.esprit.rescueops.hospital;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ms-hospital - Module 5 : Coordination hospitalière (port 8085).
 * Établissements, capacités, établissements compatibles et demandes de transfert sans double réservation.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class HospitalServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalServiceApplication.class, args);
    }
}
