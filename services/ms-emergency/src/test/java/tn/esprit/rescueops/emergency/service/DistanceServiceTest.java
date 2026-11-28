package tn.esprit.rescueops.emergency.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class DistanceServiceTest {

    private final DistanceService service =
            new DistanceService();

    @Test
    void memePositionDoitRetournerZero() {

        double distance = service.calculerDistance(
                36.8065,
                10.1815,
                36.8065,
                10.1815
        );

        assertEquals(0, distance, 0.001);
    }

    @Test
    void doitDetecterDeuxPositionsProches() {

        double distance = service.calculerDistance(
                36.8065,
                10.1815,
                36.8067,
                10.1816
        );

        assertTrue(distance > 0);
        assertTrue(distance < 100);
    }

    @Test
    void doitDetecterDeuxPositionsEloignees() {

        double distance = service.calculerDistance(
                36.8065,
                10.1815,
                36.878,
                10.3247
        );

        assertTrue(distance > 1000);
    }
}