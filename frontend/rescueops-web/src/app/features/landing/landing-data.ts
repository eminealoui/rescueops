/** Contenu de la page d'accueil publique (données de démonstration, entièrement fictives). */
import { TickerItem } from '../../core/ui/ticker';

export const TICKER_DEMO: TickerItem[] = [
  { code: 'INC-2291', texte: 'priorisé CRITIQUE' },
  { code: 'AMB-07', texte: 'en route · ETA 9 min' },
  { code: 'TRF-0412', texte: 'place retenue · Habib Thameur' },
  { code: 'Cardiologie', texte: 'La Rabta · 1 place' },
  { code: 'MSN-2288', texte: 'terminée · rapport transmis' },
  { code: 'Certification', texte: 'premiers secours délivrée' },
];

export interface Chiffre {
  valeur: number;
  suffixe?: string;
  libelle: string;
}

export const CHIFFRES: Chiffre[] = [
  { valeur: 6, libelle: 'modules métier coordonnés' },
  { valeur: 8, libelle: 'rôles, chacun son espace' },
  { valeur: 15, suffixe: ' min', libelle: 'de place retenue par transfert' },
  { valeur: 0, libelle: 'double réservation de lit' },
];

export type Ton = 'ok' | 'warn' | 'danger';
export interface LigneEcran {
  texte: string;
  /** Étiquette de statut… */
  tag?: { ton: Ton; texte: string };
  /** …ou score de compatibilité. */
  score?: { valeur: number; ton: Ton };
}

export interface Etape {
  titre: string;
  resume: string;
  accroche: string;
  detail: string;
  lignes: LigneEcran[];
}

export const ETAPES: Etape[] = [
  {
    titre: 'Signaler', resume: "Le citoyen déclare l'incident, avec sa position.", accroche: 'Un signalement en 30 secondes',
    detail: "Formulaire simple, géolocalisation, photo facultative. L'opérateur reçoit l'alerte instantanément.",
    lignes: [{ texte: 'INC-2291 · Accident · Bab Saadoun', tag: { ton: 'danger', texte: 'NOUVEAU' } }],
  },
  {
    titre: 'Prioriser', resume: "L'opérateur qualifie et priorise.", accroche: 'Une priorité calculée, une décision humaine',
    detail: "Score de priorité configurable, détection des doublons, validation par l'opérateur.",
    lignes: [
      { texte: 'INC-2291 · priorité', tag: { ton: 'danger', texte: 'CRITIQUE' } },
      { texte: 'INC-2290 · doublon probable', tag: { ton: 'warn', texte: 'À FUSIONNER' } },
    ],
  },
  {
    titre: 'Dispatcher', resume: 'La meilleure ambulance est proposée.', accroche: 'Des recommandations expliquées',
    detail: "Distance, disponibilité, compétences de l'équipe : chaque proposition est justifiée.",
    lignes: [
      { texte: 'AMB-07 · 2,1 km · équipe certifiée', score: { valeur: 94, ton: 'ok' } },
      { texte: 'AMB-03 · 4,8 km', score: { valeur: 71, ton: 'warn' } },
    ],
  },
  {
    titre: 'Intervenir', resume: "L'équipe suit sa mission en temps réel.", accroche: 'Une mission suivie étape par étape',
    detail: 'Acceptée, en route, sur place, terminée : chaque changement est horodaté et tracé.',
    lignes: [{ texte: 'MSN-2291 · AMB-07', tag: { ton: 'warn', texte: 'EN ROUTE' } }],
  },
  {
    titre: 'Hospitaliser', resume: "Une place est retenue dans l'hôpital adapté.", accroche: 'Zéro double réservation',
    detail: 'Établissements compatibles classés, place retenue 15 minutes, confirmation par le coordinateur.',
    lignes: [
      { texte: 'Habib Thameur · Trauma · 4,1 km', score: { valeur: 92, ton: 'ok' } },
      { texte: 'TRF-0412', tag: { ton: 'ok', texte: 'PLACE RETENUE · 14:21' } },
    ],
  },
];

export const MODULES = [
  { icone: '⚠', code: 'MS1', titre: 'Gestion des urgences', texte: 'Signalements, priorisation, doublons, statuts.' },
  { icone: '⛟', code: 'MS2', titre: 'Gestion des ressources', texte: 'Ambulances, équipes, équipements, maintenances.' },
  { icone: '⌖', code: 'MS3', titre: 'Dispatching intelligent', texte: 'Recommandations classées et affectations.' },
  { icone: '⏱', code: 'MS4', titre: 'Interventions', texte: 'Missions, états, itinéraires et rapports.' },
  { icone: '✚', code: 'MS5', titre: 'Coordination hospitalière', texte: 'Capacités, établissements compatibles, transferts.' },
  { icone: '✎', code: 'MS6', titre: 'Formations', texte: 'Sessions, évaluations, certifications, simulations.' },
];

export const PROFILS: { nom: string; devise: string; actions: string[] }[] = [
  { nom: 'Citoyen', devise: 'Je signale une urgence et je suis son traitement.',
    actions: ['Déclarer un incident géolocalisé', 'Suivre le statut de mon signalement', 'Gérer mon profil'] },
  { nom: 'Opérateur', devise: 'Je qualifie les alertes et je valide les affectations.',
    actions: ['Prioriser et fusionner les signalements', "Valider l'ambulance recommandée", 'Demander un transfert hospitalier'] },
  { nom: 'Ambulancier', devise: "Je mène la mission jusqu'à l'hôpital.",
    actions: ["Mettre à jour l'état de ma mission", "Réserver une place à l'hôpital adapté", "Rédiger le rapport d'intervention"] },
  { nom: 'Coordinateur hospitalier', devise: 'Je pilote les capacités et les admissions.',
    actions: ['Tenir les capacités à jour', 'Confirmer, refuser, admettre, faire sortir', 'Voir le tableau de bord régional en temps réel'] },
  { nom: 'Gestionnaire', devise: 'Je garde la flotte disponible.',
    actions: ['Gérer ambulances, équipes, équipements', 'Planifier les maintenances', 'Éviter toute double affectation'] },
  { nom: 'Formateur', devise: 'Je prépare et certifie les équipes.',
    actions: ['Créer formations et sessions', 'Évaluer et délivrer les certifications', 'Lancer des scénarios de simulation'] },
];
