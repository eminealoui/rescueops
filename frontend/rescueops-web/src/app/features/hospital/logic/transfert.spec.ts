import { Transfert } from '../../../core/api/models';
import { actionsDisponibles, compteARebours, etapeFrise, ETAPES_FRISE } from './transfert';

const base = (statut: Transfert['statut'], historique: Transfert['historique'] = null): Pick<Transfert, 'statut' | 'historique'> => ({ statut, historique });
const etape = (nouveauStatut: Transfert['statut']) => ({ ancienStatut: null, nouveauStatut, acteurUserId: 'u', horodatage: '2026-11-22T10:00:00Z', commentaire: null });

describe('frise des étapes', () => {
  it('le statut courant donne l\'étape active', () => {
    expect(etapeFrise(base('DEMANDEE'))).toEqual({ active: 0, arret: null });
    expect(etapeFrise(base('PLACE_RETENUE'))).toEqual({ active: 1, arret: null });
    expect(etapeFrise(base('ADMIS'))).toEqual({ active: 3, arret: null });
    expect(etapeFrise(base('SORTI')).active).toBe(ETAPES_FRISE.length - 1);
  });

  it('un statut d\'arrêt reprend la dernière étape atteinte dans l\'historique', () => {
    const h = [etape('DEMANDEE'), etape('PLACE_RETENUE'), etape('EXPIREE')];
    expect(etapeFrise(base('EXPIREE', h))).toEqual({ active: 1, arret: 'EXPIREE' });
    expect(etapeFrise(base('ANNULEE'))).toEqual({ active: 0, arret: 'ANNULEE' });
  });
});

describe('compte à rebours', () => {
  const maintenant = Date.parse('2026-11-22T10:00:00Z');
  it('affiche mm:ss avant expiration', () => {
    expect(compteARebours('2026-11-22T10:14:21Z', maintenant)).toEqual({ texte: '14:21', expire: false, urgent: false });
    expect(compteARebours('2026-11-22T10:00:59.400Z', maintenant)).toEqual({ texte: '00:59', expire: false, urgent: true });
  });
  it('affiche « expirée » une fois l\'échéance passée', () => {
    expect(compteARebours('2026-11-22T09:59:59Z', maintenant)).toEqual({ texte: 'expirée', expire: true, urgent: false });
  });
  it('sans échéance : rien à afficher', () => {
    expect(compteARebours(null, maintenant)).toBeNull();
  });
});

describe('actions selon le rôle et le statut', () => {
  it('le coordinateur confirme ou refuse une place retenue, et peut annuler', () => {
    expect(actionsDisponibles('PLACE_RETENUE', ['COORDINATEUR_HOSPITALIER'], false)).toEqual(['refuser', 'confirmer', 'annuler']);
  });
  it('une réservation expirée ne peut plus être confirmée', () => {
    expect(actionsDisponibles('PLACE_RETENUE', ['COORDINATEUR_HOSPITALIER'], true)).toEqual(['refuser', 'annuler']);
  });
  it("l'ambulancier ne confirme pas : il peut seulement annuler", () => {
    expect(actionsDisponibles('PLACE_RETENUE', ['AMBULANCIER'], false)).toEqual(['annuler']);
    expect(actionsDisponibles('DEMANDEE', ['AMBULANCIER'], false)).toEqual(['reserver', 'annuler']);
  });
  it('admission puis sortie, réservées au coordinateur', () => {
    expect(actionsDisponibles('CONFIRMEE', ['COORDINATEUR_HOSPITALIER'], false)).toEqual(['admettre', 'annuler']);
    expect(actionsDisponibles('ADMIS', ['COORDINATEUR_HOSPITALIER'], false)).toEqual(['sortie']);
    expect(actionsDisponibles('ADMIS', ['OPERATEUR'], false)).toEqual([]);
  });
  it('aucune action sur un statut final, ni pour un rôle sans droit', () => {
    expect(actionsDisponibles('SORTI', ['ADMIN'], false)).toEqual([]);
    expect(actionsDisponibles('DEMANDEE', ['GESTIONNAIRE_RESSOURCES'], false)).toEqual([]);
  });
});
