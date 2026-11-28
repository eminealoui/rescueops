package tn.esprit.rescueops.emergency.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SimilariteTexteServiceTest {

    private final SimilariteTexteService service =
            new SimilariteTexteService();

    @Test
    void textesIdentiques() {

        assertEquals(
                1.0,
                service.calculerSimilarite(
                        "Accident entre deux voitures",
                        "Accident entre deux voitures"
                ),
                0.001
        );
    }

    @Test
    void textesDifferents() {

        assertEquals(
                0.0,
                service.calculerSimilarite(
                        "Accident voiture",
                        "Incendie maison"
                ),
                0.001
        );
    }

    @Test
    void doitIgnorerAccentsEtMajuscules() {

        assertEquals(
                1.0,
                service.calculerSimilarite(
                        "VÉHICULE",
                        "vehicule"
                ),
                0.001
        );
    }

    @Test
    void doitGererTexteVide() {

        assertEquals(
                0.0,
                service.calculerSimilarite("", ""),
                0.001
        );
    }
}