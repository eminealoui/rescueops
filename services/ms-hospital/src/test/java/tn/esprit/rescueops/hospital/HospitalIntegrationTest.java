package tn.esprit.rescueops.hospital;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.event.RescueOpsEvent;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.hospital.domain.Gravite;
import tn.esprit.rescueops.hospital.domain.Specialite;
import tn.esprit.rescueops.hospital.domain.StatutTransfert;
import tn.esprit.rescueops.hospital.dto.*;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;
import tn.esprit.rescueops.hospital.service.EtablissementService;
import tn.esprit.rescueops.hospital.service.TransfertService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Preuves sur un VRAI MySQL 8.4 et un VRAI RabbitMQ 4.3 (mêmes versions que le socle). Nécessite Docker.
 */
@Testcontainers
@SpringBootTest(properties = {"eureka.client.enabled=false", "spring.cloud.discovery.enabled=false"})
class HospitalIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer mysql = new MySQLContainer("mysql:8.4.13");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.3.8-management");

    static final CurrentUser AMBULANCIER = new CurrentUser("amb-1", null, null, null, Set.of(Role.AMBULANCIER));
    static final CurrentUser COORDINATEUR = new CurrentUser("coord-1", null, null, null, Set.of(Role.COORDINATEUR_HOSPITALIER));

    @Autowired EtablissementService etablissements;
    @Autowired TransfertService transferts;
    @Autowired ServiceHospitalierRepository services;
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired AmqpAdmin amqpAdmin;
    @Autowired JdbcTemplate jdbc;

    private UUID nouveauService(int capacite) {
        HopitalResponse h = etablissements.creerHopital(new HopitalRequest("Hôpital " + UUID.randomUUID(), null, "Tunis", 36.8, 10.18, null));
        return etablissements.ajouterService(h.id(), new ServiceRequest(Specialite.CARDIOLOGIE, capacite)).id();
    }

    private UUID nouvelleDemande(UUID missionId) {
        return transferts.creer(new CreerTransfertRequest(missionId, Specialite.CARDIOLOGIE, Gravite.CRITIQUE, 36.8, 10.18), AMBULANCIER).id();
    }

    private int places(UUID serviceId) {
        return services.findById(serviceId).orElseThrow().getPlacesDisponibles();
    }

    @Test
    void migrationEtValidationHibernate() {
        // Le démarrage du contexte prouve que V1__schema_hospital.sql correspond aux entités (ddl-auto=validate).
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = 1", Integer.class)).isPositive();
    }

    /** BNF-05 : zéro double réservation sous concurrence. */
    @Test
    void cinquanteReservationsPourDixPlaces() throws Exception {
        UUID serviceId = nouveauService(10);
        List<UUID> demandes = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            demandes.add(nouvelleDemande(UUID.randomUUID()));
        }
        ExecutorService pool = Executors.newFixedThreadPool(50);
        CountDownLatch depart = new CountDownLatch(1);
        List<Future<String>> resultats = new ArrayList<>();
        for (UUID d : demandes) {
            resultats.add(pool.submit(() -> {
                depart.await();
                try {
                    transferts.reserver(d, serviceId, AMBULANCIER);
                    return "OK";
                } catch (ConflictException e) {
                    return e.code();
                }
            }));
        }
        depart.countDown();
        List<String> codes = new ArrayList<>();
        for (Future<String> f : resultats) {
            codes.add(f.get(60, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertThat(codes).filteredOn("OK"::equals).hasSize(10);
        assertThat(codes).filteredOn("CAPACITE_EPUISEE"::equals).hasSize(40);
        assertThat(places(serviceId)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM demande_transfert WHERE service_id = UUID_TO_BIN(?) AND statut = 'PLACE_RETENUE'",
                Integer.class, serviceId.toString())).isEqualTo(10);
    }

    /** Une expiration et un refus simultanés : une seule transition gagne, la place n'est rendue qu'une fois. */
    @Test
    void expirationEtRefusSimultanesRendentUneSeulePlace() throws Exception {
        UUID serviceId = nouveauService(5);
        UUID demande = nouvelleDemande(UUID.randomUUID());
        transferts.reserver(demande, serviceId, AMBULANCIER);
        assertThat(places(serviceId)).isEqualTo(4);
        jdbc.update("UPDATE demande_transfert SET expire_at = ? WHERE id = UUID_TO_BIN(?)",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), demande.toString());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);
        Future<Boolean> expiration = pool.submit(() -> {
            depart.await();
            try {
                return transferts.expirer(demande);
            } catch (RuntimeException e) {
                return false;
            }
        });
        Future<Boolean> refus = pool.submit(() -> {
            depart.await();
            try {
                transferts.refuser(demande, "complet", COORDINATEUR);
                return true;
            } catch (ConflictException e) {
                return false;
            }
        });
        depart.countDown();
        boolean expiree = expiration.get(30, TimeUnit.SECONDS), refusee = refus.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(expiree ^ refusee).as("exactement une des deux transitions réussit").isTrue();
        assertThat(places(serviceId)).isEqualTo(5);
    }

    @Test
    void evenementPublieApresCommit() {
        Queue espion = new Queue("test.espion." + UUID.randomUUID(), true, false, false);
        amqpAdmin.declareQueue(espion);
        amqpAdmin.declareBinding(BindingBuilder.bind(espion).to(new TopicExchange("rescueops.events")).with("transfert.#"));
        UUID serviceId = nouveauService(3);
        UUID demande = nouvelleDemande(UUID.randomUUID());

        transferts.reserver(demande, serviceId, AMBULANCIER);
        transferts.confirmer(demande, COORDINATEUR);

        List<String> cles = new ArrayList<>();
        await().atMost(Duration.ofSeconds(10)).until(() -> {
            Message m = rabbitTemplate.receive(espion.getName(), 500);
            if (m != null) {
                cles.add(m.getMessageProperties().getReceivedRoutingKey());
            }
            return cles.contains("transfert.confirme");
        });
        amqpAdmin.deleteQueue(espion.getName());
        assertThat(cles).containsSubsequence("transfert.place_retenue", "transfert.confirme");
    }

    /** BNF-08 : le même mission.terminee livré deux fois n'a qu'un seul effet. */
    @Test
    void missionTermineeDeuxFoisUnSeulEffet() {
        UUID serviceId = nouveauService(4);
        UUID mission = UUID.randomUUID();
        UUID demande = nouvelleDemande(mission);
        transferts.reserver(demande, serviceId, AMBULANCIER);
        assertThat(places(serviceId)).isEqualTo(3);

        RescueOpsEvent<Map<String, Object>> evt = new RescueOpsEvent<>(UUID.randomUUID(), "mission.terminee", 1,
                Instant.now(), null, Map.of("missionId", mission.toString()));
        rabbitTemplate.convertAndSend("rescueops.events", "mission.terminee", evt);
        rabbitTemplate.convertAndSend("rescueops.events", "mission.terminee", evt);

        await().atMost(Duration.ofSeconds(15)).until(() ->
                transferts.trouver(demande).statut() == StatutTransfert.ANNULEE);
        await().pollDelay(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> true);

        assertThat(places(serviceId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM historique_transfert WHERE demande_id = UUID_TO_BIN(?) AND nouveau_statut = 'ANNULEE'",
                Integer.class, demande.toString())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM evenement_traite WHERE event_id = UUID_TO_BIN(?)",
                Integer.class, evt.eventId().toString())).isEqualTo(1);
    }
}
