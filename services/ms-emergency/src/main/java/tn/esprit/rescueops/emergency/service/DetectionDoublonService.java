package tn.esprit.rescueops.emergency.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.emergency.domain.Incident;
import tn.esprit.rescueops.emergency.domain.StatutIncident;
import tn.esprit.rescueops.emergency.dto.DoublonResponse;
import tn.esprit.rescueops.emergency.repository.IncidentRepository;

@Service
public class DetectionDoublonService {

    private final IncidentRepository repository;
    private final DistanceService distanceService;
    private final SimilariteTexteService texteService;

    public DetectionDoublonService(
            IncidentRepository repository,
            DistanceService distanceService,
            SimilariteTexteService texteService
    ) {

        this.repository = repository;
        this.distanceService = distanceService;
        this.texteService = texteService;
    }

    @Transactional(readOnly = true)
    public List<DoublonResponse> detecter(UUID id) {

        Incident source = repository.findById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Incident introuvable"
                        )
                );

        if (source.getLatitude() == null
                || source.getLongitude() == null
                || source.getStatut() == StatutIncident.REJETE
                || source.getStatut() == StatutIncident.CLOTURE
                || source.getStatut() == StatutIncident.FUSIONNE) {

            return List.of();
        }

        List<Incident> candidats =
                repository.findByIdNotAndCreeLeBetweenAndStatutIn(

                        id,

                        source.getCreeLe()
                                .minus(Duration.ofHours(1)),

                        source.getCreeLe()
                                .plus(Duration.ofHours(1)),

                        List.of(
                                StatutIncident.SIGNALE,
                                StatutIncident.VALIDE,
                                StatutIncident.EN_COURS
                        )
                );

        return candidats.stream()

                .filter(i -> i.getLatitude() != null
                        && i.getLongitude() != null)

                .map(i -> comparer(source, i))

                .filter(Objects::nonNull)

                .filter(i -> i.scoreSimilarite() >= 60)

                .sorted(
                        Comparator.comparingInt(
                                DoublonResponse::scoreSimilarite
                        ).reversed()
                )

                .limit(10)

                .toList();
    }

    private DoublonResponse comparer(
            Incident source,
            Incident candidat
    ) {

        double distance =
                distanceService.calculerDistance(

                        source.getLatitude(),
                        source.getLongitude(),

                        candidat.getLatitude(),
                        candidat.getLongitude()
                );

        if (distance > 1000) {
            return null;
        }

        long minutes = Math.abs(
                Duration.between(
                        source.getCreeLe(),
                        candidat.getCreeLe()
                ).toMinutes()
        );

        double similarite =
                texteService.calculerSimilarite(
                        source.getDescription(),
                        candidat.getDescription()
                );

        int scoreDistance;

        if (distance <= 100) {
            scoreDistance = 40;
        } else if (distance <= 500) {
            scoreDistance = 30;
        } else {
            scoreDistance = 15;
        }

        int scoreTemps;

        if (minutes <= 10) {
            scoreTemps = 20;
        } else if (minutes <= 30) {
            scoreTemps = 15;
        } else {
            scoreTemps = 5;
        }

        int scoreType =
                source.getType() != null
                && source.getType() == candidat.getType()
                        ? 20 : 0;

        int scoreTexte =
                (int) Math.round(similarite * 20);

        int scoreTotal =
                scoreDistance
                + scoreTemps
                + scoreType
                + scoreTexte;

        String recommandation = scoreTotal >= 80
                ? "DOUBLON_TRES_PROBABLE"
                : "DOUBLON_POSSIBLE";

        return new DoublonResponse(

                candidat.getId(),
                candidat.getTitre(),
                candidat.getDescription(),
                candidat.getType(),
                candidat.getStatut(),
                candidat.getCreeLe(),

                distance,
                minutes,
                similarite,
                scoreTotal,
                recommandation
        );
    }
}