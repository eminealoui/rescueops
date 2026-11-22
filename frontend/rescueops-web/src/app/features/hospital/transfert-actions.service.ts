import { Injectable, inject } from '@angular/core';
import { EMPTY, Observable, filter, switchMap } from 'rxjs';
import { Transfert, codeCourt } from '../../core/api/models';
import { TransferApi } from '../../core/api/hospital.api';
import { Confirm } from '../../core/ui/confirm';
import { ToastService } from '../../core/ui/toast.service';
import { LiveFeed } from '../../layout/live-feed.service';
import { ActionTransfert } from './logic/transfert';

/** Exécute une action sur un transfert (avec confirmation si elle est irréversible) et annonce le résultat. */
@Injectable({ providedIn: 'root' })
export class TransfertActions {
  private readonly api = inject(TransferApi);
  private readonly confirm = inject(Confirm);
  private readonly toast = inject(ToastService);
  private readonly feed = inject(LiveFeed);

  /** Émet le transfert mis à jour, ou rien si l'utilisateur annule. 'reserver' se fait depuis l'écran Nouvelle demande. */
  executer(action: ActionTransfert, t: Transfert): Observable<Transfert> {
    const code = codeCourt('TRF', t.id);
    let appel$: Observable<Transfert>;
    switch (action) {
      case 'confirmer':
        appel$ = this.api.confirmer(t.id);
        break;
      case 'admettre':
        appel$ = this.api.admettre(t.id);
        break;
      case 'sortie':
        appel$ = this.demander({ titre: 'Déclarer la sortie du patient ?', texte: `${code} · la place est rendue au service.`, action: 'Déclarer la sortie' },
          () => this.api.sortie(t.id));
        break;
      case 'refuser':
        appel$ = this.confirm
          .demander({ titre: 'Refuser ce transfert ?', texte: `${code} · la place retenue est libérée.`, action: 'Refuser', danger: true,
            champ: 'Motif du refus (facultatif)' })
          .pipe(filter((m): m is string => m !== null), switchMap((m) => this.api.refuser(t.id, m || null)));
        break;
      case 'annuler':
        appel$ = this.demander({ titre: 'Annuler cette demande ?', texte: `${code} · une place retenue serait libérée.`, action: 'Annuler la demande', danger: true },
          () => this.api.annuler(t.id));
        break;
      default:
        return EMPTY;
    }
    return new Observable<Transfert>((obs) =>
      appel$.subscribe({
        next: (maj) => {
          this.toast.ok(MESSAGES[action](maj));
          this.feed.rafraichir();
          obs.next(maj);
        },
        error: (e) => obs.error(e),
        complete: () => obs.complete(),
      }),
    );
  }

  private demander(d: Parameters<Confirm['demander']>[0], appel: () => Observable<Transfert>): Observable<Transfert> {
    return this.confirm.demander(d).pipe(filter((r) => r !== null), switchMap(appel));
  }
}

const MESSAGES: Record<ActionTransfert, (t: Transfert) => string> = {
  reserver: (t) => `Place retenue · ${t.hopitalNom ?? ''}`,
  confirmer: (t) => `Transfert ${codeCourt('TRF', t.id)} confirmé · ${t.hopitalNom ?? ''}`,
  refuser: () => 'Transfert refusé · la place est libérée',
  admettre: () => 'Patient admis',
  sortie: () => 'Sortie déclarée · la place est rendue',
  annuler: () => 'Demande annulée',
};
