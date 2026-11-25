import { Role } from '../core/auth/roles';

export interface EntreeNav {
  label: string;
  path: string;
  /** Vide = tout utilisateur connecté. */
  roles: readonly Role[];
}

/**
 * Onglets de l'en-tête, filtrés selon les rôles. Chaque coéquipier remplace la ligne de SON module
 * (path « /modules/msX » → sa propre route) quand ses écrans sont prêts.
 */
export const NAVIGATION: readonly EntreeNav[] = [
  { label: 'Accueil', path: '/accueil', roles: [] },
  { label: 'Urgences', path: '/modules/ms1', roles: ['CITOYEN', 'OPERATEUR', 'ADMIN'] },
  { label: 'Ressources', path: '/modules/ms2', roles: ['GESTIONNAIRE_RESSOURCES', 'ADMIN'] },
  { label: 'Dispatch', path: '/dispatch', roles: ['OPERATEUR', 'ADMIN'] },
  { label: 'Interventions', path: '/modules/ms4', roles: ['OPERATEUR', 'AMBULANCIER', 'ADMIN'] },
  { label: 'Hôpitaux', path: '/hopitaux', roles: ['OPERATEUR', 'AMBULANCIER', 'COORDINATEUR_HOSPITALIER', 'ADMIN'] },
  { label: 'Formations', path: '/modules/ms6', roles: ['FORMATEUR', 'APPRENANT', 'ADMIN'] },
  { label: 'Administration', path: '/admin/utilisateurs', roles: ['ADMIN'] },
];
