package tn.esprit.rescueops.hospital.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Réglages métier du module (application.yml, préfixe rescueops.hospital).
 *
 * @param reservationTtl      durée d'une place retenue avant expiration automatique
 * @param expiryCheckInterval fréquence de la tâche d'expiration
 * @param maxRadiusKm         rayon de recherche des établissements compatibles
 */
@ConfigurationProperties("rescueops.hospital")
public record HospitalProperties(
        @DefaultValue("15m") Duration reservationTtl,
        @DefaultValue("30s") Duration expiryCheckInterval,
        @DefaultValue("50") double maxRadiusKm) {
}
