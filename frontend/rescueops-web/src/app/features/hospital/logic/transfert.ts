/** Règles d'affichage d'une demande de transfert (reflet de la machine à états de ms-hospital). */
import { StatutTransfert, Transfert } from '../../../core/api/models';
import { Role } from '../../../core/auth/roles';

export const ETAPES_FRISE: readonly StatutTransfert[] = ['DEMANDEE', 'PLACE_RETENUE', 'CONFIRMEE', 'ADMIS', 'SORTI'];
const ARRETS: readonly StatutTransfert[] = ['REFUSEE', 'EXPIREE', 'ANNULEE'];

export interface Frise {
  /** Index dans ETAPES_FRISE de la dernière étape atteinte. */
  active: number;
  /** Statut d'arrêt (refus, expiration, annulation), sinon null. */
  arret: StatutTransfert | null;
}

export function etapeFrise(t: Pick<Transfert, 'statut' | 'historique'>): Frise {
  const i = ETAPES_FRISE.indexOf(t.statut);
  if (i >= 0) return { active: i, arret: null };
  const atteintes = (t.historique ?? []).map((h) => ETAPES_FRISE.indexOf(h.nouveauStatut)).filter((x) => x >= 0);
  return { active: atteintes.length ? Math.max(...atteintes) : 0, arret: ARRETS.includes(t.statut) ? t.statut : null };
}

export interface CompteARebours {
  texte: string;
  expire: boolean;
  /** Moins d'une minute restante. */
  urgent: boolean;
}

export function compteARebours(expireAt: string | null, maintenant: number): CompteARebours | null {
  if (!expireAt) return null;
  const reste = Math.floor((Date.parse(expireAt) - maintenant) / 1000);
  if (reste < 0) return { texte: 'expirée', expire: true, urgent: false };
  const mm = String(Math.floor(reste / 60)).padStart(2, '0');
  const ss = String(reste % 60).padStart(2, '0');
  return { texte: `${mm}:${ss}`, expire: false, urgent: reste < 60 };
}

export type ActionTransfert = 'reserver' | 'refuser' | 'confirmer' | 'admettre' | 'sortie' | 'annuler';

const HOPITAL: readonly Role[] = ['COORDINATEUR_HOSPITALIER', 'ADMIN'];
const RESERVATION: readonly Role[] = ['AMBULANCIER', 'OPERATEUR', 'COORDINATEUR_HOSPITALIER', 'ADMIN'];
const ANNULABLE: readonly StatutTransfert[] = ['DEMANDEE', 'PLACE_RETENUE', 'CONFIRMEE'];

/**
 * Boutons proposés, dans l'ordre d'affichage. Ergonomie seulement : l'API refuse de toute façon une action interdite
 * (rôle, statut, propriétaire de la demande).
 */
export function actionsDisponibles(statut: StatutTransfert, roles: readonly Role[], expire: boolean): ActionTransfert[] {
  const a = (r: readonly Role[]) => r.some((x) => roles.includes(x));
  const actions: ActionTransfert[] = [];
  if (statut === 'DEMANDEE' && a(RESERVATION)) actions.push('reserver');
  if (statut === 'PLACE_RETENUE' && a(HOPITAL)) {
    actions.push('refuser');
    if (!expire) actions.push('confirmer');
  }
  if (statut === 'CONFIRMEE' && a(HOPITAL)) actions.push('admettre');
  if (statut === 'ADMIS' && a(HOPITAL)) actions.push('sortie');
  if (ANNULABLE.includes(statut) && a(RESERVATION)) actions.push('annuler');
  return actions;
}

export const LIBELLES_ACTION: Record<ActionTransfert, string> = {
  reserver: 'Réserver une place',
  refuser: 'Refuser',
  confirmer: 'Confirmer',
  admettre: 'Admettre',
  sortie: 'Déclarer la sortie',
  annuler: 'Annuler la demande',
};

/** Ton de l'étiquette d'un statut (vert / ambre / rouge réservés aux statuts). */
export function tonStatut(s: StatutTransfert): 'ok' | 'warn' | 'danger' | 'neutre' {
  switch (s) {
    case 'CONFIRMEE':
    case 'ADMIS':
      return 'ok';
    case 'DEMANDEE':
    case 'PLACE_RETENUE':
      return 'warn';
    case 'REFUSEE':
    case 'EXPIREE':
      return 'danger';
    default:
      return 'neutre';
  }
}
