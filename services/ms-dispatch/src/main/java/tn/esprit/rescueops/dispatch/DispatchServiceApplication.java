package tn.esprit.rescueops.dispatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ms-dispatch - Module 3 : Dispatching intelligent.
 * <p>
 * Port 8083. Sécurité, erreurs et événements sont fournis automatiquement par rescueops-common.
 */
@SpringBootApplication
@EnableFeignClients
public class DispatchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DispatchServiceApplication.class, args);
    }
}
