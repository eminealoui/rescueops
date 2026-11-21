package tn.esprit.rescueops.hospital.event;

import tn.esprit.rescueops.hospital.domain.StatutTransfert;

import java.util.UUID;

/** Événement interne : une demande a changé de statut. Publié sur RabbitMQ (transfert.*) après commit. */
public record TransfertStatutChange(UUID transfertId, UUID missionId, UUID serviceId, StatutTransfert statut) {
}
