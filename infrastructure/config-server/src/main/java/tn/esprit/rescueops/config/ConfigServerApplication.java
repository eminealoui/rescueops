package tn.esprit.rescueops.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Serveur de configuration centralisée : sert aux microservices les fichiers du dossier
 * config-repo/ (application.yml commun + un ms-xxx.yml par service).
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
