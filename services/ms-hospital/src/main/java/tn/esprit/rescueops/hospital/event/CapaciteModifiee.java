package tn.esprit.rescueops.hospital.event;

import java.util.UUID;

/** Événement interne : une capacité a changé. Publié sur RabbitMQ (capacite.maj) après commit. */
public record CapaciteModifiee(UUID serviceId) {
}
