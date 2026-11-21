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
    private final HospitalMapper mapper;
    private final ApplicationEventPublisher events;

    public EtablissementService(HopitalRepository hopitaux, ServiceHospitalierRepository services,
                                HistoriqueCapaciteRepository historique, HospitalMapper mapper,
                                ApplicationEventPublisher events) {
        this.hopitaux = hopitaux;
        this.services = services;
        this.historique = historique;
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

    @Transactional
    public ServiceResponse changerEtatService(UUID serviceId, boolean actif) {
        ServiceHospitalier s = chargerService(serviceId);
        s.setActif(actif);
        events.publishEvent(new CapaciteModifiee(serviceId));
        return mapper.toResponse(s);
    }

    /**
     * Ajustement manuel. Règles : 0 ≤ places ≤ capacité, et la nouvelle capacité ne peut pas être inférieure
     * aux places déjà occupées. Écriture « comparer puis écrire » : si une réservation passe entre la lecture
     * et l'écriture, on refuse (409) au lieu d'écraser sa place.
     */
    @Transactional
    public ServiceResponse ajusterCapacite(UUID serviceId, CapaciteRequest req, String acteur) {
        ServiceHospitalier s = chargerService(serviceId);
        int ancienneCapacite = s.getCapaciteTotale();
        int anciennesPlaces = s.getPlacesDisponibles();
        int occupees = ancienneCapacite - anciennesPlaces;
        if (req.placesDisponibles() > req.capaciteTotale()) {
            throw new BusinessRuleException("CAPACITE_INVALIDE",
                    "Les places disponibles (" + req.placesDisponibles() + ") dépassent la capacité (" + req.capaciteTotale() + ").");
        }
        if (req.capaciteTotale() < occupees) {
            throw new BusinessRuleException("CAPACITE_INVALIDE",
                    "La capacité ne peut pas être inférieure aux " + occupees + " places occupées.");
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
