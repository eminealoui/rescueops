package tn.esprit.rescueops.hospital.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.rescueops.common.error.BusinessRuleException;
import tn.esprit.rescueops.common.error.ConflictException;
import tn.esprit.rescueops.common.error.NotFoundException;
import tn.esprit.rescueops.hospital.domain.HistoriqueCapacite;
import tn.esprit.rescueops.hospital.domain.Hopital;
import tn.esprit.rescueops.hospital.domain.ServiceHospitalier;
import tn.esprit.rescueops.hospital.dto.*;
import tn.esprit.rescueops.hospital.event.CapaciteModifiee;
import tn.esprit.rescueops.hospital.mapper.HospitalMapper;
import tn.esprit.rescueops.hospital.repository.DemandeTransfertRepository;
import tn.esprit.rescueops.hospital.repository.HistoriqueCapaciteRepository;
import tn.esprit.rescueops.hospital.repository.HopitalRepository;
import tn.esprit.rescueops.hospital.repository.ServiceHospitalierRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** BF-13 : établissements, services et capacités. */
@Service
public class EtablissementService {

    private final HopitalRepository hopitaux;
    private final ServiceHospitalierRepository services;
    private final HistoriqueCapaciteRepository historique;
    private final DemandeTransfertRepository demandes;
    private final HospitalMapper mapper;
    private final ApplicationEventPublisher events;

    public EtablissementService(HopitalRepository hopitaux, ServiceHospitalierRepository services,
                                HistoriqueCapaciteRepository historique, DemandeTransfertRepository demandes,
                                HospitalMapper mapper, ApplicationEventPublisher events) {
        this.hopitaux = hopitaux;
        this.services = services;
        this.historique = historique;
        this.demandes = demandes;
        this.mapper = mapper;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public List<HopitalResponse> listerHopitaux() {
        return hopitaux.findAllByOrderByNomAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public HopitalResponse trouverHopital(UUID id) {
        return mapper.toResponse(chargerHopital(id));
    }

    @Transactional
    public HopitalResponse creerHopital(HopitalRequest req) {
        Hopital h = new Hopital();
        appliquer(h, req);
        return mapper.toResponse(hopitaux.save(h));
    }

    @Transactional
    public HopitalResponse modifierHopital(UUID id, HopitalRequest req) {
        Hopital h = chargerHopital(id);
        appliquer(h, req);
        return mapper.toResponse(h);
    }

    /** Suppression logique : l'hôpital n'est plus proposé, mais les transferts passés restent consultables. */
    @Transactional
    public void supprimerHopital(UUID id) {
        chargerHopital(id).setActif(false);
    }

    @Transactional
    public ServiceResponse ajouterService(UUID hopitalId, ServiceRequest req) {
        ServiceHospitalier s = new ServiceHospitalier();
        s.setHopital(chargerHopital(hopitalId));
        s.setSpecialite(req.specialite());
        s.setCapaciteTotale(req.capaciteTotale());
        s.setPlacesDisponibles(req.capaciteTotale());
        s.setActif(true);
        ServiceResponse r = mapper.toResponse(services.save(s));
        events.publishEvent(new CapaciteModifiee(r.id()));
        return r;
    }

    /** Ouvre ou ferme un service par une requête ciblée : le compteur de places n'est jamais réécrit. */
    @Transactional
    public ServiceResponse changerEtatService(UUID serviceId, boolean actif) {
        chargerService(serviceId);
        services.changerActif(serviceId, actif);
        events.publishEvent(new CapaciteModifiee(serviceId));
        return mapper.toResponse(chargerService(serviceId));
    }

    /**
     * Ajustement manuel (ex. patients arrivés ou sortis hors RescueOps).
     * Règle : on ne peut jamais déclarer libres des lits tenus par des transferts RescueOps
     * (RETENUE, CONFIRMEE, ADMIS) : places ≤ capacité − lits tenus par RescueOps.
     * La ligne est verrouillée (FOR UPDATE) pendant le calcul : aucune réservation ne peut s'intercaler.
     */
    @Transactional
    public ServiceResponse ajusterCapacite(UUID serviceId, CapaciteRequest req, String acteur) {
        ServiceHospitalier s = services.findByIdPourMiseAJour(serviceId)
                .orElseThrow(() -> new NotFoundException("Service hospitalier introuvable."));
        int ancienneCapacite = s.getCapaciteTotale();
        int anciennesPlaces = s.getPlacesDisponibles();
        long tenuesParRescueOps = demandes.compterPlacesOccupees(serviceId);
        if (req.placesDisponibles() > req.capaciteTotale()) {
            throw new BusinessRuleException("CAPACITE_INVALIDE",
                    "Les places disponibles (" + req.placesDisponibles() + ") dépassent la capacité (" + req.capaciteTotale() + ").");
        }
        if (req.placesDisponibles() > req.capaciteTotale() - tenuesParRescueOps) {
            throw new BusinessRuleException("CAPACITE_INVALIDE", tenuesParRescueOps
                    + " lit(s) sont tenus par des transferts en cours : au plus "
                    + Math.max(0, req.capaciteTotale() - tenuesParRescueOps) + " place(s) libre(s) pour une capacité de "
                    + req.capaciteTotale() + ".");
        }
        int lignes = services.ajusterCapacite(serviceId, ancienneCapacite, anciennesPlaces,
                req.capaciteTotale(), req.placesDisponibles());
        if (lignes == 0) {
            throw new ConflictException("MODIFICATION_CONCURRENTE",
                    "La capacité vient de changer (réservation en cours). Rechargez puis réessayez.");
        }
        HistoriqueCapacite h = new HistoriqueCapacite();
        h.setServiceId(serviceId);
        h.setAncienneCapacite(ancienneCapacite);
        h.setNouvelleCapacite(req.capaciteTotale());
        h.setAnciennesPlaces(anciennesPlaces);
        h.setNouvellesPlaces(req.placesDisponibles());
        h.setActeurUserId(acteur);
        h.setHorodatage(Instant.now());
        h.setMotif(req.motif());
        historique.save(h);
        events.publishEvent(new CapaciteModifiee(serviceId));
        return new ServiceResponse(serviceId, s.getHopital().getId(), s.getSpecialite(), req.capaciteTotale(),
                req.placesDisponibles(), s.isActif());
    }

    private Hopital chargerHopital(UUID id) {
        return hopitaux.findById(id).orElseThrow(() -> new NotFoundException("Hôpital introuvable."));
    }

    private ServiceHospitalier chargerService(UUID id) {
        return services.findById(id).orElseThrow(() -> new NotFoundException("Service hospitalier introuvable."));
    }

    private static void appliquer(Hopital h, HopitalRequest req) {
        h.setNom(req.nom().trim());
        h.setAdresse(req.adresse());
        h.setVille(req.ville().trim());
        h.setLatitude(req.latitude());
        h.setLongitude(req.longitude());
        h.setTelephone(req.telephone() == null || req.telephone().isBlank() ? null : req.telephone().trim());
    }
}
