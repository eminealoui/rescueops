
package tn.esprit.rescueops.intervention.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CalculItineraireServiceTest {

    @Test
    void calculerDistanceEtDureeAvecOsrm() throws Exception {

        // Simuler localement une reponse du serveur OSRM
        HttpServer serveur = HttpServer.create(
            new InetSocketAddress("127.0.0.1", 0),
            0
        );

        serveur.createContext("/route/v1/driving/", exchange -> {

            String json = """
                    {
                      "code": "Ok",
                      "routes": [
                        {
                          "distance": 2670.0,
                          "duration": 281.0
                        }
                      ]
                    }
                    """;

            byte[] contenu = json.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type", "application/json"
            );

            exchange.sendResponseHeaders(200, contenu.length);

            try (var sortie = exchange.getResponseBody()) {
                sortie.write(contenu);
            }
        });

        serveur.start();

        try {

            String url = "http://127.0.0.1:"
                + serveur.getAddress().getPort();

            CalculItineraireService service =
                new CalculItineraireService(url);

            var resultat = service.calculer(
                new BigDecimal("36.8065"),
                new BigDecimal("10.1815"),
                new BigDecimal("36.8189"),
                new BigDecimal("10.1658")
            );

            assertEquals(2.67, resultat.distanceKm(), 0.001);

            assertEquals(5, resultat.dureeEstimeeMinutes());

        } finally {
            serveur.stop(0);
        }
    }
}
