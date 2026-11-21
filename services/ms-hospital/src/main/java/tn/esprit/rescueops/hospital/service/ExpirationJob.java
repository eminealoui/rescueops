package tn.esprit.rescueops.hospital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Libère automatiquement les places retenues dont le délai est dépassé.
 * Chaque demande est traitée dans SA propre transaction (TransfertService.expirer) avec une transition
 * conditionnelle : sûr même avec 2 instances du service ou un refus simultané, sans verrou distribué.
 */
@Component
public class ExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(ExpirationJob.class);
    private static final int LOT = 100;

    private final DemandeTransfertRepository demandes;
    private final TransfertService transferts;

    public ExpirationJob(DemandeTransfertRepository demandes, TransfertService transferts) {
        this.demandes = demandes;
        this.transferts = transferts;
    }

    @Scheduled(fixedDelayString = "${rescueops.hospital.expiry-check-interval:30s}")
    public void planifie() {
        int n = executer();
        if (n > 0) {
            log.info("{} réservation(s) expirée(s), place(s) rendue(s)", n);
        }
    }

    /** @return le nombre de demandes réellement expirées par cette exécution */
    public int executer() {
        int expirees = 0;
        for (UUID id : demandes.idsEchus(Instant.now(), PageRequest.of(0, LOT))) {
            try {
                if (transferts.expirer(id)) {
                    expirees++;
                }
            } catch (tn.esprit.rescueops.common.error.ConflictException e) {
                // Déjà traitée ailleurs (refus, autre instance) : normal.
                log.debug("Expiration ignorée pour {} : {}", id, e.getMessage());
            } catch (RuntimeException e) {
                // Vraie erreur (base indisponible...) : visible, mais on continue avec les autres demandes.
                log.warn("Échec de l'expiration de la demande {}", id, e);
            }
        }
        return expirees;
    }
}
