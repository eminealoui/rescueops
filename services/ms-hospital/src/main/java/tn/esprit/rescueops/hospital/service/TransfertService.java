package tn.esprit.rescueops.hospital.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.ForbiddenOperationException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.common.security.CurrentUser;
import tn.esprit.rescueops.common.security.Role;
import tn.esprit.rescueops.hospital.config.HospitalProperties;
import tn.esprit.rescueops.hospital.domain.*;
import tn.esprit.rescueops.hospital.dto.CreerTransfertRequest;
import tn.esprit.rescueops.hospital.dto.TransfertResponse;
import tn.esprit.rescueops.hospital.event.CapaciteModifiee;
import tn.esprit.rescueops.hospital.event.TransfertStatutChange;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.HistoriqueTransfertRepository;
import tn.esprit.rescueops.hospital.repository.MissionTermineeRepository;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * BF-15 : cycle de vie des demandes de transfert.
 * Chaque opération = une transaction : transition conditionnelle, mouvement de place atomique,
 * historique, puis événement (publié sur RabbitMQ seulement après commit).
 * Ordre des verrous : toujours le SERVICE avant la DEMANDE quand les deux sont modifiés (sinon interblocage MySQL).
 */
@Service
public class TransfertService {

    private static final Logger log = LoggerFactory.getLogger(TransfertService.class);

    /** Acteur technique inscrit dans l'historique pour les actions automatiques. */
    public static final String SYSTEME = "systeme";

    /** Statuts d'une demande encore « en cours » : une seule par mission. */
    static final Set<StatutTransfert> OUVERTS = Set.of(StatutTransfert.DEMANDEE, StatutTransfert.PLACE_RETENUE,
            StatutTransfert.CONFIRMEE, StatutTransfert.ADMIS);

    private final DemandeTransfertRepository demandes;
    private final ServiceHospitalierRepository services;
    private final HistoriqueTransfertRepository historique;
    private final MissionTermineeRepository missionsTerminees;
    private final ApplicationEventPublisher events;
    private final HospitalProperties props;

    public TransfertService(DemandeTransfertRepository demandes, ServiceHospitalierRepository services,
                            HistoriqueTransfertRepository historique, MissionTermineeRepository missionsTerminees,
                            ApplicationEventPublisher events, HospitalProperties props) {
        this.demandes = demandes;
        this.services = services;
        this.historique = historique;
        this.missionsTerminees = missionsTerminees;
        this.events = events;
        this.props = props;
    }

    @Transactional
    public TransfertResponse creer(CreerTransfertRequest req, CurrentUser user) {
        if (missionsTerminees.existsById(req.missionId())) {
            throw new ConflictException("MISSION_TERMINEE", "Cette mission est terminée : aucun transfert ne peut être demandé.");
        }
        if (demandes.existsByMissionIdAndStatutIn(req.missionId(), OUVERTS)) {
            throw new ConflictException("DEMANDE_EN_COURS", "Une demande de transfert est déjà en cours pour cette mission.");
        }
        DemandeTransfert d = new DemandeTransfert();
        d.setMissionId(req.missionId());
        d.setSpecialiteRequise(req.specialiteRequise());
        d.setGravite(req.gravite());
        d.setLatitudeOrigine(req.latitudeOrigine());
        d.setLongitudeOrigine(req.longitudeOrigine());
        d.setStatut(StatutTransfert.DEMANDEE);
        d.setDemandeurUserId(user.keycloakId());
        d = demandes.save(d);
        tracer(d.getId(), null, StatutTransfert.DEMANDEE, user.keycloakId(), null);
        events.publishEvent(new TransfertStatutChange(d.getId(), d.getMissionId(), null, StatutTransfert.DEMANDEE));
        return toResponse(d, false);
    }

    /**
     * DEMANDEE → PLACE_RETENUE. Contrôles : la demande est à soi (ou coordinateur / admin), le service existe et
     * correspond à la spécialité (ou au repli URGENCES_GENERALES). La place est prise atomiquement ;
     * s'il n'y en a plus, tout est annulé (409).
     */
    @Transactional
    public TransfertResponse reserver(UUID id, UUID serviceId, CurrentUser user) {
        DemandeTransfert d = charger(id);
        verifierProprietaireOuHopital(d, user, "réserver");
        if (d.getStatut() != StatutTransfert.DEMANDEE) {
            throw transitionInterdite(d.getStatut(), "réserver");
        }
        ServiceHospitalier cible = services.findById(serviceId)
                .orElseThrow(() -> new NotFoundException("Service hospitalier introuvable."));
        if (cible.getSpecialite() != d.getSpecialiteRequise() && cible.getSpecialite() != Specialite.URGENCES_GENERALES) {
            throw new BusinessRuleException("SERVICE_INCOMPATIBLE", "Ce service (" + cible.getSpecialite()
                    + ") ne correspond pas à la spécialité demandée (" + d.getSpecialiteRequise() + ").");
        }
        if (services.reserverPlace(serviceId) == 0) {
            throw new ConflictException("CAPACITE_EPUISEE", "Plus aucune place disponible dans ce service.");
        }
        Instant maintenant = Instant.now();
        if (demandes.retenirPlace(id, serviceId, maintenant.plus(props.reservationTtl()), user.keycloakId(), maintenant) == 0) {
            // Une autre action a traité la demande entre-temps : exception → rollback, la place est rendue.
            throw transitionInterdite(charger(id).getStatut(), "réserver");
        }
        tracer(id, StatutTransfert.DEMANDEE, StatutTransfert.PLACE_RETENUE, user.keycloakId(), null);
        events.publishEvent(new TransfertStatutChange(id, d.getMissionId(), serviceId, StatutTransfert.PLACE_RETENUE));
        events.publishEvent(new CapaciteModifiee(serviceId));
        return toResponse(charger(id), false);
    }

    /** PLACE_RETENUE → CONFIRMEE, seulement avant l'échéance de la réservation. */
    @Transactional
    public TransfertResponse confirmer(UUID id, CurrentUser user) {
        DemandeTransfert avant = charger(id);
        if (avant.getStatut() != StatutTransfert.PLACE_RETENUE) {
            throw transitionInterdite(avant.getStatut(), "confirmer");
        }
        if (demandes.confirmer(id, user.keycloakId(), Instant.now()) == 0) {
            DemandeTransfert actuelle = charger(id);
            if (actuelle.getStatut() == StatutTransfert.PLACE_RETENUE) {
                throw new ConflictException("RESERVATION_EXPIREE",
                        "Le délai de réservation est dépassé : la place va être libérée. Refaites une réservation.");
            }
            throw transitionInterdite(actuelle.getStatut(), "confirmer");
        }
        apresTransition(charger(id), StatutTransfert.PLACE_RETENUE, StatutTransfert.CONFIRMEE, user.keycloakId(), null, false);
        return toResponse(charger(id), false);
    }

    @Transactional
    public TransfertResponse refuser(UUID id, String motif, CurrentUser user) {
        return transition(id, StatutTransfert.PLACE_RETENUE, StatutTransfert.REFUSEE, user.keycloakId(), motif, true);
    }

    @Transactional
    public TransfertResponse admettre(UUID id, CurrentUser user) {
        return transition(id, StatutTransfert.CONFIRMEE, StatutTransfert.ADMIS, user.keycloakId(), null, false);
    }

    /** Sortie du patient : la place est rendue. */
    @Transactional
    public TransfertResponse sortie(UUID id, CurrentUser user) {
        return transition(id, StatutTransfert.ADMIS, StatutTransfert.SORTI, user.keycloakId(), null, true);
    }

    /** Annulation par le demandeur, un coordinateur ou un admin ; rend la place si elle était prise. */
    @Transactional
    public TransfertResponse annuler(UUID id, CurrentUser user) {
        DemandeTransfert d = charger(id);
        verifierProprietaireOuHopital(d, user, "annuler");
        if (!StatutTransfert.ANNULABLE.contains(d.getStatut())) {
            throw transitionInterdite(d.getStatut(), "annuler");
        }
        return transition(id, d.getStatut(), StatutTransfert.ANNULEE, user.keycloakId(), null,
                StatutTransfert.OCCUPE_UNE_PLACE.contains(d.getStatut()));
    }

    /**
     * Annulation automatique (fin de mission), dans SA propre transaction (pas d'interblocage avec d'autres demandes).
     * Le statut est RELU à chaque tentative : si une réservation vient d'aboutir, la place est quand même rendue.
     *
     * @return true si la demande a été annulée par cet appel
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean annulerAutomatiquement(UUID id) {
        for (int tentative = 0; tentative < 3; tentative++) {
            StatutTransfert actuel = charger(id).getStatut();
            if (!StatutTransfert.ANNULABLE.contains(actuel)) {
                return false;
            }
            if (demandes.changerStatut(id, actuel, StatutTransfert.ANNULEE, SYSTEME, null, Instant.now()) == 1) {
                apresTransition(charger(id), actuel, StatutTransfert.ANNULEE, SYSTEME, "fin de mission",
                        StatutTransfert.OCCUPE_UNE_PLACE.contains(actuel));
                return true;
            }
        }
        return false;
    }

    /** PLACE_RETENUE → EXPIREE si l'échéance est passée. Retourne false si une autre action l'a déjà traitée. */
    @Transactional
    public boolean expirer(UUID id) {
        if (demandes.expirer(id, Instant.now()) == 0) {
            return false;
        }
        apresTransition(charger(id), StatutTransfert.PLACE_RETENUE, StatutTransfert.EXPIREE, SYSTEME, "délai dépassé", true);
        return true;
    }

    @Transactional(readOnly = true)
    public TransfertResponse trouver(UUID id) {
        return toResponse(charger(id), true);
    }

    @Transactional(readOnly = true)
    public List<TransfertResponse> lister(StatutTransfert statut, int page, int size) {
        PageRequest p = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        var resultat = statut == null ? demandes.findAll(p) : demandes.findByStatut(statut, p);
        return resultat.getContent().stream().map(d -> toResponse(d, false)).toList();
    }

    private TransfertResponse transition(UUID id, StatutTransfert attendu, StatutTransfert nouveau, String acteur,
                                         String motif, boolean rendreLaPlace) {
        DemandeTransfert avant = charger(id);
        if (demandes.changerStatut(id, attendu, nouveau, acteur, motif, Instant.now()) == 0) {
            throw transitionInterdite(avant.getStatut(), verbe(nouveau));
        }
        apresTransition(charger(id), attendu, nouveau, acteur, motif, rendreLaPlace);
        // Relecture : rendrePlace vide le contexte JPA, l'entité précédente est détachée.
        return toResponse(charger(id), false);
    }

    private void apresTransition(DemandeTransfert d, StatutTransfert ancien, StatutTransfert nouveau, String acteur,
                                 String commentaire, boolean rendreLaPlace) {
        UUID serviceId = d.getService() == null ? null : d.getService().getId();
        if (rendreLaPlace && serviceId != null) {
            if (services.rendrePlace(serviceId) == 0) {
                // Ne devrait jamais arriver (compteur déjà au maximum) : incohérence à investiguer, jamais silencieuse.
                log.error("Place non rendue pour la demande {} : le service {} est déjà à sa capacité maximale", d.getId(), serviceId);
            }
            events.publishEvent(new CapaciteModifiee(serviceId));
        }
        tracer(d.getId(), ancien, nouveau, acteur, commentaire);
        events.publishEvent(new TransfertStatutChange(d.getId(), d.getMissionId(), serviceId, nouveau));
    }

    /** Une demande n'est manipulée que par son auteur, un coordinateur hospitalier ou un admin. */
    private static void verifierProprietaireOuHopital(DemandeTransfert d, CurrentUser user, String action) {
        boolean autorise = user.keycloakId().equals(d.getDemandeurUserId())
                || user.has(Role.COORDINATEUR_HOSPITALIER) || user.has(Role.ADMIN);
        if (!autorise) {
            throw new ForbiddenOperationException(
                    "Seul le demandeur, un coordinateur hospitalier ou un admin peut " + action + " cette demande.");
        }
    }

    private void tracer(UUID demandeId, StatutTransfert ancien, StatutTransfert nouveau, String acteur, String commentaire) {
        HistoriqueTransfert h = new HistoriqueTransfert();
        h.setDemandeId(demandeId);
        h.setAncienStatut(ancien);
        h.setNouveauStatut(nouveau);
        h.setActeurUserId(acteur);
        h.setHorodatage(Instant.now());
        h.setCommentaire(commentaire);
        historique.save(h);
    }

    private DemandeTransfert charger(UUID id) {
        return demandes.findById(id).orElseThrow(() -> new NotFoundException("Demande de transfert introuvable."));
    }

    private static ConflictException transitionInterdite(StatutTransfert actuel, String action) {
        return new ConflictException("TRANSITION_INTERDITE",
                "Impossible de " + action + " : la demande est à l'état " + actuel + ".");
    }

    private static String verbe(StatutTransfert cible) {
        return switch (cible) {
            case CONFIRMEE -> "confirmer";
            case REFUSEE -> "refuser";
            case ADMIS -> "admettre";
            case SORTI -> "déclarer la sortie";
            case ANNULEE -> "annuler";
            default -> "changer le statut";
        };
    }

    private TransfertResponse toResponse(DemandeTransfert d, boolean avecHistorique) {
        ServiceHospitalier s = d.getService();
        List<TransfertResponse.Etape> etapes = avecHistorique
                ? historique.findByDemandeIdOrderByHorodatageAsc(d.getId()).stream()
                .map(h -> new TransfertResponse.Etape(h.getAncienStatut(), h.getNouveauStatut(), h.getActeurUserId(),
                        h.getHorodatage(), h.getCommentaire())).toList()
                : null;
        return new TransfertResponse(d.getId(), d.getMissionId(), d.getSpecialiteRequise(), d.getGravite(),
                d.getLatitudeOrigine(), d.getLongitudeOrigine(), s == null ? null : s.getId(),
                s == null || s.getHopital() == null ? null : s.getHopital().getNom(), d.getStatut(), d.getExpireAt(),
                d.getDemandeurUserId(), d.getTraiteParUserId(), d.getMotifRefus(), d.getCreatedAt(), etapes);
    }
}
