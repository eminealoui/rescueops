package tn.esprit.rescueops.emergency.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class SimilariteTexteService {

    private static final Set<String> MOTS_VIDES =
            Set.of(
                    "le", "la", "les", "un", "une",
                    "des", "de", "du", "dans", "et",
                    "a", "au", "aux", "en", "avec",
                    "sur", "est", "entre"
            );

    private Set<String> normaliser(String texte) {

        if (texte == null || texte.isBlank()) {
            return new HashSet<>();
        }

        String normalise = Normalizer.normalize(
                texte.toLowerCase(Locale.ROOT),
                Normalizer.Form.NFD
        );

        normalise = normalise
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ");

        return Arrays.stream(normalise.split("\\s+"))
                .filter(mot -> !mot.isBlank())
                .filter(mot -> !MOTS_VIDES.contains(mot))
                .collect(Collectors.toSet());
    }

    public double calculerSimilarite(
            String texte1,
            String texte2
    ) {

        Set<String> mots1 = normaliser(texte1);
        Set<String> mots2 = normaliser(texte2);

        Set<String> intersection =
                new HashSet<>(mots1);

        intersection.retainAll(mots2);

        Set<String> union = new HashSet<>(mots1);

        union.addAll(mots2);

        if (union.isEmpty()) {
            return 0;
        }

        return (double) intersection.size()
                / union.size();
    }
}