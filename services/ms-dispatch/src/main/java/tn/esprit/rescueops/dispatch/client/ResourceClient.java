package tn.esprit.rescueops.dispatch.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Client Feign vers ms-resource (port 8082, préfixe /api/resources).
 * <p>
 * BF-07 : récupérer les ressources disponibles pour alimenter l'algorithme de scoring.
 * BF-08 : réserver la ressource sélectionnée lors de la confirmation d'une affectation.
 * BF-09 : libérer la ressource en cas d'annulation.
 * <p>
 * Contrat : on accède à ms-resource via son API REST, jamais via sa base de données.
 */
@FeignClient(name = "ms-resource", path = "/api/resources")
public interface ResourceClient {

    /**
     * Récupère la liste des ressources disponibles, optionnellement filtrées par type.
     *
     * @param type filtre optionnel : AMBULANCE, EQUIPE, EQUIPEMENT
     * @return liste de ressources disponibles avec leurs coordonnées GPS et compétences
     */
    @GetMapping("/available")
    List<ResourceDto> getAvailableResources(@RequestParam(required = false) String type);

    /**
     * Réserve une ressource identifiée par son ID.
     * Appelé lors de la confirmation d'une affectation (BF-08).
     *
     * @param id identifiant de la ressource à réserver
     */
    @PostMapping("/{id}/reserve")
    void reserverRessource(@PathVariable String id);

    /**
     * Libère une ressource précédemment réservée.
     * Appelé lors de l'annulation d'une affectation (BF-09).
     *
     * @param id identifiant de la ressource à libérer
     */
    @PostMapping("/{id}/release")
    void libererRessource(@PathVariable String id);

    /**
     * DTO représentant une ressource retournée par ms-resource.
     * Dénormalisé : contient les informations nécessaires au scoring.
     */
    record ResourceDto(
            String id,
            String nom,
            String type,          // AMBULANCE, EQUIPE, EQUIPEMENT
            Boolean disponible,
            Double latitude,
            Double longitude,
            List<String> competences
    ) {}
}
