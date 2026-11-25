/** Types des réponses de l'API, recopiés des records Java (ms-user, ms-hospital). Dates : chaînes ISO 8601 UTC. */
import { Role } from '../auth/roles';

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// ---------- ms-user ----------
export interface UserProfile {
  keycloakId: string;
  email: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  address: string | null;
  organisation: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdateMyProfile {
  firstName: string;
  lastName: string;
  phone: string | null;
  address: string | null;
}

export type StatutDemandeRole = 'EN_ATTENTE' | 'ACCEPTEE' | 'REFUSEE';

export interface DemandeRole {
  id: string;
  keycloakId: string;
  roleDemande: Role;
  organisation: string;
  motif: string | null;
  statut: StatutDemandeRole;
  motifRefus: string | null;
  createdAt: string;
  traiteLe: string | null;
}

export interface NouvelleDemandeRole {
  role: Role;
  organisation: string;
  motif: string | null;
}

// ---------- ms-hospital ----------
export const SPECIALITES = [
  'REANIMATION', 'TRAUMATOLOGIE', 'CARDIOLOGIE', 'PEDIATRIE', 'NEUROLOGIE', 'BRULES', 'URGENCES_GENERALES', 'MATERNITE',
] as const;
export type Specialite = (typeof SPECIALITES)[number];

export const LIBELLES_SPECIALITE: Record<Specialite, string> = {
  REANIMATION: 'Réanimation',
  TRAUMATOLOGIE: 'Traumatologie',
  CARDIOLOGIE: 'Cardiologie',
  PEDIATRIE: 'Pédiatrie',
  NEUROLOGIE: 'Neurologie',
  BRULES: 'Brûlés',
  URGENCES_GENERALES: 'Urgences générales',
  MATERNITE: 'Maternité',
};

export const GRAVITES = ['FAIBLE', 'MOYENNE', 'ELEVEE', 'CRITIQUE'] as const;
export type Gravite = (typeof GRAVITES)[number];
export const LIBELLES_GRAVITE: Record<Gravite, string> = { FAIBLE: 'Faible', MOYENNE: 'Moyenne', ELEVEE: 'Élevée', CRITIQUE: 'Critique' };

export const STATUTS_TRANSFERT = ['DEMANDEE', 'PLACE_RETENUE', 'CONFIRMEE', 'ADMIS', 'SORTI', 'REFUSEE', 'EXPIREE', 'ANNULEE'] as const;
export type StatutTransfert = (typeof STATUTS_TRANSFERT)[number];
export const LIBELLES_STATUT: Record<StatutTransfert, string> = {
  DEMANDEE: 'Demandée',
  PLACE_RETENUE: 'Place retenue',
  CONFIRMEE: 'Confirmée',
  ADMIS: 'Admis',
  SORTI: 'Sorti',
  REFUSEE: 'Refusée',
  EXPIREE: 'Expirée',
  ANNULEE: 'Annulée',
};

export type EtatService = 'DISPONIBLE' | 'RESERVE' | 'SATURE';

export interface ServiceHospitalier {
  id: string;
  hopitalId: string;
  specialite: Specialite;
  capaciteTotale: number;
  placesDisponibles: number;
  actif: boolean;
}

export interface Hopital {
  id: string;
  nom: string;
  adresse: string | null;
  ville: string;
  latitude: number;
  longitude: number;
  telephone: string | null;
  actif: boolean;
  services: ServiceHospitalier[];
}

export interface HopitalRequest {
  nom: string;
  adresse: string | null;
  ville: string;
  latitude: number;
  longitude: number;
  telephone: string | null;
}

export interface TableauDeBord {
  kpis: { placesLibres: number; reservationsActives: number; servicesSatures: number; tauxAcceptation24h: number | null };
  services: LigneService[];
}

export interface LigneService {
  serviceId: string;
  hopitalId: string;
  hopitalNom: string;
  ville: string;
  specialite: Specialite;
  capaciteTotale: number;
  placesDisponibles: number;
  placesRetenues: number;
  etat: EtatService;
}

export interface Candidat {
  serviceId: string;
  hopitalId: string;
  hopitalNom: string;
  specialite: Specialite;
  placesDisponibles: number;
  capaciteTotale: number;
  distanceKm: number | null;
  score: number;
  sousScores: { distance: number; disponibilite: number; specialite: number };
  justification: string;
}

export interface EtapeTransfert {
  ancienStatut: StatutTransfert | null;
  nouveauStatut: StatutTransfert;
  acteurUserId: string;
  horodatage: string;
  commentaire: string | null;
}

export interface Transfert {
  id: string;
  missionId: string;
  specialiteRequise: Specialite;
  gravite: Gravite;
  latitudeOrigine: number | null;
  longitudeOrigine: number | null;
  serviceId: string | null;
  hopitalNom: string | null;
  statut: StatutTransfert;
  expireAt: string | null;
  demandeurUserId: string;
  traiteParUserId: string | null;
  motifRefus: string | null;
  createdAt: string;
  historique: EtapeTransfert[] | null;
}

export interface CreerTransfert {
  missionId: string;
  specialiteRequise: Specialite;
  gravite: Gravite;
  latitudeOrigine: number | null;
  longitudeOrigine: number | null;
}

export interface EvenementFlux {
  transfertId: string;
  statut: StatutTransfert;
  hopitalNom: string | null;
  horodatage: string;
}

/** Code court lisible d'un identifiant UUID (ex. « TRF-3F2A »). */
export function codeCourt(prefixe: string, id: string | number): string {
  return `${prefixe}-${String(id).replace(/-/g, '').slice(0, 4).toUpperCase()}`;
}

// ---------- ms-dispatch ----------
export const STATUTS_AFFECTATION = ['PROPOSEE', 'CONFIRMEE', 'ANNULEE'] as const;
export type StatutAffectation = (typeof STATUTS_AFFECTATION)[number];
export const LIBELLES_STATUT_AFFECTATION: Record<StatutAffectation, string> = {
  PROPOSEE: 'Proposée',
  CONFIRMEE: 'Confirmée',
  ANNULEE: 'Annulée',
};

/** Corps envoyé à ms-dispatch. Le score reste exprimé en 0–100 côté UI, DispatchApi le convertit en 0–1 pour l'API. */
export interface AffectationRequest {
  incidentId: string;
  ressourceId: string;
  typeRessource?: string | null;
  score?: number;
}

export interface AffectationResponse {
  /** Identifiant technique : Long côté Java, reçu comme nombre en JSON. */
  id: number | string;
  incidentId: string;
  ressourceId: string;
  typeRessource: string | null;
  /** Score ramené en 0–100 par DispatchApi (le backend stocke 0–1). */
  score: number | null;
  justification: string | null;
  operateurId: string | null;
  statut: StatutAffectation;
  createdAt: string;
  updatedAt: string;
}

export interface RecommandationRequest {
  incidentId: string;
  latitudeIncident: number | null;
  longitudeIncident: number | null;
  typeRessource: string | null;
}

export interface RecommandationResponse {
  id: number | string;
  incidentId: string;
  ressourceId: string;
  nomRessource: string | null;
  /** Score ramené en 0–100 par DispatchApi (le backend stocke 0–1). */
  score: number;
  distanceKm: number | null;
  disponible: boolean;
  competencesOk: boolean;
  typeRessource: string | null;
  justification: string | null;
}
