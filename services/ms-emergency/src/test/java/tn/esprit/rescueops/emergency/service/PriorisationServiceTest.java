package tn.esprit.rescueops.emergency.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import tn.esprit.rescueops.emergency.domain.NiveauGravite;
import tn.esprit.rescueops.emergency.domain.NiveauPriorite;
import tn.esprit.rescueops.emergency.domain.TypeIncident;

class PriorisationServiceTest {

    private final PriorisationService service =
            new PriorisationService();

    @Test
    void doitCalculerPrioriteCritique() {

        var resultat = service.calculer(
                TypeIncident.INCENDIE,
                NiveauGravite.TRES_GRAVE,
                5, true, true
        );

        assertEquals(100, resultat.score());
        assertEquals(NiveauPriorite.CRITIQUE,
                resultat.priorite());
    }

    @Test
    void doitCalculerPrioriteHaute() {

        var resultat = service.calculer(
                TypeIncident.ACCIDENT_ROUTIER,
                NiveauGravite.GRAVE,
                3, true, false
        );

        assertEquals(70, resultat.score());
        assertEquals(NiveauPriorite.HAUTE,
                resultat.priorite());
    }

    @Test
    void doitCalculerPrioriteFaible() {

        var resultat = service.calculer(
                TypeIncident.AUTRE,
                NiveauGravite.MINEURE,
                0, false, false
        );

        assertEquals(10, resultat.score());
        assertEquals(NiveauPriorite.FAIBLE,
                resultat.priorite());
    }

    @Test
    void doitGererNombreVictimesInconnu() {

        var resultat = service.calculer(
                TypeIncident.AUTRE,
                NiveauGravite.MINEURE,
                null, false, false
        );

        assertEquals(10, resultat.score());
        assertTrue(resultat.explication()
                .contains("inconnu"));
    }

    @Test
    void scoreNeDoitPasDepasser100() {

        var resultat = service.calculer(
                TypeIncident.INCENDIE,
                NiveauGravite.TRES_GRAVE,
                100, true, true
        );

        assertEquals(100, resultat.score());
    }
}