
package tn.esprit.rescueops.intervention.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Service
public class CalculItineraireService {

    private final RestClient restClient;
    private final String baseUrl;

    public record Estimation(
        double distanceKm,
        int dureeEstimeeMinutes
    ) {}

    public CalculItineraireService(
        @Value("${itineraire.osrm.base-url:https://router.project-osrm.org}")
        String baseUrl
    ) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");

        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();


        JdkClientHttpRequestFactory factory =
            new JdkClientHttpRequestFactory(httpClient);

        factory.setReadTimeout(Duration.ofSeconds(8));

    // Eviter la decompression automatique gzip/deflate
            factory.enableCompression(false);



        this.restClient = RestClient.builder()
            .requestFactory(factory)
            .defaultHeader(
                HttpHeaders.ACCEPT_ENCODING,
                "identity"
            )
            .defaultHeader(
                HttpHeaders.ACCEPT,
                MediaType.APPLICATION_JSON_VALUE
            )
            .build();

    }

    public Estimation calculer(
        BigDecimal latitudeDepart,
        BigDecimal longitudeDepart,
        BigDecimal latitudeDestination,
        BigDecimal longitudeDestination
    ) {

        String coordonnees =
            longitudeDepart.toPlainString() + ","
                + latitudeDepart.toPlainString() + ";"
                + longitudeDestination.toPlainString() + ","
                + latitudeDestination.toPlainString();

        URI uri = URI.create(
            baseUrl + "/route/v1/driving/"
                + coordonnees + "?overview=false"
        );

        Map<?, ?> reponse;



        try {
            reponse = restClient.get()
                .uri(uri)
                .retrieve()
                .body(Map.class);

        } catch (RestClientException e) {
            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service de calcul d'itineraire indisponible",
                e
            );
        }


        if (reponse == null
            || !"Ok".equals(reponse.get("code"))
            || !(reponse.get("routes") instanceof List<?> routes)
            || routes.isEmpty()) {

            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Aucun itineraire routier disponible"
            );
        }

        if (!(routes.get(0) instanceof Map<?, ?> trajet)
            || !(trajet.get("distance") instanceof Number distance)
            || !(trajet.get("duration") instanceof Number duree)) {

            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Reponse du service de routage invalide"
            );
        }

        double distanceMetres = distance.doubleValue();
        double dureeSecondes = duree.doubleValue();

        if (!Double.isFinite(distanceMetres)
            || !Double.isFinite(dureeSecondes)
            || distanceMetres < 0
            || dureeSecondes < 0
            || dureeSecondes / 60 > Integer.MAX_VALUE) {

            throw new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Estimation routiere invalide"
            );
        }

        double distanceKm =
            Math.round(distanceMetres / 10.0) / 100.0;

        int dureeMinutes =
            (int) Math.ceil(dureeSecondes / 60.0);

        return new Estimation(
            distanceKm,
            dureeMinutes
        );
    }
}
