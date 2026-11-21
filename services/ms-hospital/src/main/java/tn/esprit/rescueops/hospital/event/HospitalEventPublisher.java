package tn.esprit.rescueops.hospital.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publie les événements du module sur rescueops.events, SEULEMENT après le commit en base :
 * jamais d'annonce d'un transfert qui aurait finalement été annulé par un rollback.
 * Payloads : uniquement des identifiants et des statuts (aucune donnée personnelle).
 */
@Component
public class HospitalEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(HospitalEventPublisher.class);

    private final RabbitTemplate rabbit;
    private final ServiceHospitalierRepository services;

    public HospitalEventPublisher(RabbitTemplate rabbit, ServiceHospitalierRepository services) {
        this.rabbit = rabbit;
        this.services = services;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surTransition(TransfertStatutChange e) {
        String cle = switch (e.statut()) {
            case PLACE_RETENUE -> "transfert.place_retenue";
            case CONFIRMEE -> "transfert.confirme";
            case REFUSEE -> "transfert.refuse";
            case EXPIREE -> "transfert.expire";
            default -> null;   // les autres statuts restent internes au module
        };
        if (cle == null) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("transfertId", e.transfertId().toString());
        payload.put("missionId", e.missionId().toString());
        payload.put("serviceId", e.serviceId() == null ? null : e.serviceId().toString());
        payload.put("statut", e.statut().name());
        envoyer(cle, payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void surCapacite(CapaciteModifiee e) {
        services.findById(e.serviceId()).ifPresent(s -> {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("serviceId", s.getId().toString());
            payload.put("specialite", s.getSpecialite().name());
            payload.put("placesDisponibles", s.getPlacesDisponibles());
            payload.put("capaciteTotale", s.getCapaciteTotale());
            payload.put("actif", s.isActif());
            envoyer("capacite.maj", payload);
        });
    }

    /**
     * Le commit a déjà eu lieu : une panne de RabbitMQ ne doit pas transformer une opération réussie en erreur.
     * Limite connue (documentée) : sans pattern Outbox, l'événement est alors perdu et seulement journalisé.
     */
    private void envoyer(String cle, Map<String, Object> payload) {
        try {
            rabbit.convertAndSend(cle, RescueOpsEvent.of(cle, payload));
        } catch (AmqpException ex) {
            log.error("Événement {} non publié (RabbitMQ indisponible) : {}", cle, payload, ex);
        }
    }
}
