package tn.esprit.rescueops.hospital.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.hospital.config.HospitalProperties;
import tn.esprit.rescueops.hospital.domain.Gravite;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;
import tn.esprit.rescueops.hospital.domain.Specialite;
import tn.esprit.rescueops.hospital.dto.CandidatDto;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * BF-14 : établissements compatibles avec un transfert, classés par un score expliqué.
 * <ul>
 *   <li>Candidats : services actifs avec au moins une place, de la spécialité demandée ou URGENCES_GENERALES (repli),
 *       dans le rayon maximal.</li>
 *   <li>Sous-scores (0-100) : distance = 100 × (1 − d / rayon) ; disponibilité = 100 × libres / capacité ;
 *       spécialité = 100 exacte, 40 repli.</li>
 *   <li>Poids (distance / disponibilité / spécialité) selon la gravité : CRITIQUE 0,6/0,1/0,3 ;
 *       ELEVEE 0,5/0,2/0,3 ; MOYENNE et FAIBLE 0,35/0,35/0,3.</li>
 * </ul>
 * La décision finale reste humaine : ce score n'est qu'une aide (fiche de cadrage).
 */
@Service
public class CompatibiliteService {

    private static final double RAYON_TERRE_KM = 6371.0;
    private static final int SCORE_REPLI = 40;

    private final ServiceHospitalierRepository services;
    private final HospitalProperties props;

    public CompatibiliteService(ServiceHospitalierRepository services, HospitalProperties props) {
        this.services = services;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<CandidatDto> proposer(Specialite specialite, Double lat, Double lon, Gravite gravite) {
        boolean positionConnue = lat != null && lon != null;
        double[] poids = poids(gravite);
        return services.findActifsAvecHopital().stream()
                .filter(s -> s.getPlacesDisponibles() > 0 && s.getCapaciteTotale() > 0)
                .filter(s -> s.getSpecialite() == specialite || s.getSpecialite() == Specialite.URGENCES_GENERALES)
                .map(s -> noter(s, specialite, positionConnue ? distanceKm(lat, lon, s) : null, poids))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(CandidatDto::score).reversed()
                        .thenComparing(c -> c.distanceKm() == null ? Double.MAX_VALUE : c.distanceKm()))
                .toList();
    }

    private CandidatDto noter(ServiceHospitalier s, Specialite demandee, Double distance, double[] poids) {
        double rayon = props.maxRadiusKm();
        if (distance != null && distance > rayon) {
            return null;
        }
        int sDistance = distance == null ? 0 : (int) Math.round(100 * (1 - distance / rayon));
        int sDispo = (int) Math.round(100.0 * s.getPlacesDisponibles() / s.getCapaciteTotale());
        boolean exacte = s.getSpecialite() == demandee;
        int sSpecialite = exacte ? 100 : SCORE_REPLI;
        int score = (int) Math.round(poids[0] * sDistance + poids[1] * sDispo + poids[2] * sSpecialite);

        String justification = (distance == null ? "distance inconnue" : String.format(Locale.FRANCE, "%.1f km", distance))
                + " · " + sDispo + " % de places libres"
                + " · " + (exacte ? "spécialité exacte" : "repli urgences générales");
        Double distanceArrondie = distance == null ? null : Math.round(distance * 10) / 10.0;
        return new CandidatDto(s.getId(), s.getHopital().getId(), s.getHopital().getNom(), s.getSpecialite(),
                s.getPlacesDisponibles(), s.getCapaciteTotale(), distanceArrondie, Math.clamp(score, 0, 100),
                new CandidatDto.SousScores(sDistance, sDispo, sSpecialite), justification);
    }

    private static double[] poids(Gravite gravite) {
        return switch (gravite) {
            case CRITIQUE -> new double[]{0.6, 0.1, 0.3};
            case ELEVEE -> new double[]{0.5, 0.2, 0.3};
            case MOYENNE, FAIBLE -> new double[]{0.35, 0.35, 0.3};
        };
    }

    /** Distance à vol d'oiseau (formule de Haversine). */
    static double distanceKm(double lat, double lon, ServiceHospitalier s) {
        double dLat = Math.toRadians(s.getHopital().getLatitude() - lat);
        double dLon = Math.toRadians(s.getHopital().getLongitude() - lon);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat)) * Math.cos(Math.toRadians(s.getHopital().getLatitude()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * RAYON_TERRE_KM * Math.asin(Math.sqrt(a));
    }
}
