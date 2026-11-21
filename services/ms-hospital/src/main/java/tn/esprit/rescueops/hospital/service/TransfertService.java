package tn.esprit.rescueops.hospital.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * BF-15 : cycle de vie des demandes de transfert.
 * Chaque opération = une transaction : transition conditionnelle, mouvement de place atomique,
 * historique, puis événement (publié sur RabbitMQ seulement après commit).
 */
@Service
public class TransfertService {

    /** Acteur technique inscrit dans l'historique pour les actions automatiques. */
    public static final String SYSTEME = "systeme";

    private final DemandeTransfertRepository demandes;
    private final ServiceHospitalierRepository services;
    private final HistoriqueTransfertRepository historique;
    private final ApplicationEventPublisher events;
    private final HospitalProperties props;

    public TransfertService(DemandeTransfertRepository demandes, ServiceHospitalierRepository services,
                            HistoriqueTransfertRepository historique, ApplicationEventPublisher events,
                            HospitalProperties props) {
        this.demandes = demandes;
        this.services = services;
        this.historique = historique;
        this.events = events;
        this.props = props;
    }

    @Transactional
    public TransfertResponse creer(CreerTransfertRequest req, CurrentUser user) {
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

    /** DEMANDEE → PLACE_RETENUE. La place est prise atomiquement ; s'il n'y en a plus, tout est annulé (409). */
    @Transactional
    public TransfertResponse reserver(UUID id, UUID serviceId, CurrentUser user) {
        DemandeTransfert d = charger(id);
        Instant maintenant = Instant.now();
        if (demandes.retenirPlace(id, serviceId, maintenant.plus(props.reservationTtl()), user.keycloakId(), maintenant) == 0) {
            throw transitionInterdite(d.getStatut(), "réserver");
        }
        if (services.reserverPlace(serviceId) == 0) {
            // Exception → rollback : la demande redevient DEMANDEE, aucune place n'est prise.
            throw new ConflictException("CAPACITE_EPUISEE", "Plus aucune place disponible dans ce service.");
        }
        tracer(id, StatutTransfert.DEMANDEE, StatutTransfert.PLACE_RETENUE, user.keycloakId(), null);
        events.publishEvent(new TransfertStatutChange(id, d.getMissionId(), serviceId, StatutTransfert.PLACE_RETENUE));
        events.publishEvent(new CapaciteModifiee(serviceId));
        return toResponse(charger(id), false);
    }

    @Transactional
    public TransfertResponse confirmer(UUID id, CurrentUser user) {
        return transition(id, StatutTransfert.PLACE_RETENUE, StatutTransfert.CONFIRMEE, user.keycloakId(), null, false);
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
        boolean autorise = user.keycloakId().equals(d.getDemandeurUserId())
                || user.has(Role.COORDINATEUR_HOSPITALIER) || user.has(Role.ADMIN);
        if (!autorise) {
            throw new ForbiddenOperationException("Seul le demandeur, un coordinateur ou un admin peut annuler cette demande.");
        }
        if (!StatutTransfert.ANNULABLE.contains(d.getStatut())) {
            throw transitionInterdite(d.getStatut(), "annuler");
        }
        return transition(id, d.getStatut(), StatutTransfert.ANNULEE, user.keycloakId(), null,
                StatutTransfert.OCCUPE_UNE_PLACE.contains(d.getStatut()));
    }

    /** Annulation automatique (fin de mission). Retourne false si la demande n'était plus annulable. */
    @Transactional
    public boolean annulerAutomatiquement(UUID id, StatutTransfert statutActuel) {
        if (demandes.changerStatut(id, statutActuel, StatutTransfert.ANNULEE, SYSTEME, null) == 0) {
            return false;
        }
        apresTransition(charger(id), statutActuel, StatutTransfert.ANNULEE, SYSTEME, "fin de mission",
                StatutTransfert.OCCUPE_UNE_PLACE.contains(statutActuel));
        return true;
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
        if (demandes.changerStatut(id, attendu, nouveau, acteur, motif) == 0) {
            throw transitionInterdite(avant.getStatut(), verbe(nouveau));
        }
        DemandeTransfert apres = charger(id);
        apresTransition(apres, attendu, nouveau, acteur, motif, rendreLaPlace);
        return toResponse(apres, false);
    }

    private void apresTransition(DemandeTransfert d, StatutTransfert ancien, StatutTransfert nouveau, String acteur,
                                 String commentaire, boolean rendreLaPlace) {
        UUID serviceId = d.getService() == null ? null : d.getService().getId();
        if (rendreLaPlace && serviceId != null) {
            services.rendrePlace(serviceId);
            events.publishEvent(new CapaciteModifiee(serviceId));
        }
        tracer(d.getId(), ancien, nouveau, acteur, commentaire);
        events.publishEvent(new TransfertStatutChange(d.getId(), d.getMissionId(), serviceId, nouveau));
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
