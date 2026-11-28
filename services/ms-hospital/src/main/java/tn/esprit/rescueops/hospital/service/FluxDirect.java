package tn.esprit.rescueops.hospital.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tn.esprit.rescueops.hospital.event.TransfertStatutChange;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bandeau « EN DIRECT » en temps réel (Server-Sent Events) : chaque navigateur abonné reçoit
 * la liste à jour dès qu'un transfert change de statut, au lieu d'interroger l'API toutes les 15 s.
 * <ul>
 *   <li>à l'abonnement : la liste actuelle est envoyée tout de suite ;</li>
 *   <li>après chaque changement validé en base (AFTER_COMMIT) : nouvelle liste pour tous ;</li>
 *   <li>toutes les 15 s : un commentaire « ping » garde la connexion ouverte à travers Nginx et la Gateway ;</li>
 *   <li>au bout de 10 min : la connexion est fermée, le navigateur se reconnecte avec un jeton frais.</li>
 * </ul>
 * Mêmes données que GET /feed : aucune donnée personnelle.
 * Limite : les abonnés d'une instance ne voient que les changements faits sur cette instance.
 */
@Service
public class FluxDirect {

    static final long DUREE_CONNEXION_MS = Duration.ofMinutes(10).toMillis();
    private static final Logger log = LoggerFactory.getLogger(FluxDirect.class);

    private final FluxService flux;
    private final Set<SseEmitter> abonnes = new CopyOnWriteArraySet<>();
    // Un seul fil pour diffuser : un navigateur lent ne bloque jamais la requête qui a modifié le transfert.
    private final ExecutorService diffusion = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "flux-direct");
        t.setDaemon(true);
        return t;
    });

    public FluxDirect(FluxService flux) {
        this.flux = flux;
    }

    public SseEmitter abonner() {
        SseEmitter emitter = new SseEmitter(DUREE_CONNEXION_MS);
        abonnes.add(emitter);
        emitter.onCompletion(() -> abonnes.remove(emitter));
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> abonnes.remove(emitter));
        envoyer(emitter, flux.derniers(null));
        log.debug("Flux direct : {} abonné(s)", abonnes.size());
        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void surChangement(TransfertStatutChange evenement) {
        if (!abonnes.isEmpty()) {
            diffusion.execute(this::diffuser);
        }
    }

    @Scheduled(fixedRate = 15_000)
    public void battement() {
        for (SseEmitter emitter : abonnes) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (Exception e) {
                abonnes.remove(emitter);
            }
        }
    }

    int nombreAbonnes() {
        return abonnes.size();
    }

    void diffuser() {
        List<FluxService.Evenement> liste = flux.derniers(null);
        for (SseEmitter emitter : abonnes) {
            envoyer(emitter, liste);
        }
    }

    private void envoyer(SseEmitter emitter, List<FluxService.Evenement> liste) {
        try {
            emitter.send(SseEmitter.event().name("flux").data(liste, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            // Navigateur parti : on l'oublie, il se reconnectera s'il revient.
            abonnes.remove(emitter);
        }
    }

    @PreDestroy
    void fermer() {
        diffusion.shutdownNow();
        abonnes.forEach(SseEmitter::complete);
    }
}
