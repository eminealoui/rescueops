package tn.esprit.rescueops.emergency.service;

import org.springframework.stereotype.Service;

@Service
public class DistanceService {

    private static final double RAYON_TERRE = 6371000;

    public double calculerDistance(
            double lat1,
            double lon1,
            double lat2,
            double lon2
    ) {

        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a =
                Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(deltaLon / 2), 2);

        a = Math.max(0, Math.min(1, a));

        double c = 2 * Math.asin(Math.sqrt(a));

        return RAYON_TERRE * c;
    }
}