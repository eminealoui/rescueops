import { A11yModule } from '@angular/cdk/a11y';
import { Dialog } from '@angular/cdk/dialog';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { LIBELLES_GRAVITE, LIBELLES_SPECIALITE, LIBELLES_STATUT, Transfert, codeCourt } from '../../core/api/models';
import { AuthService } from '../../core/auth/auth.service';
import { ActionTransfert, LIBELLES_ACTION, actionsDisponibles, compteARebours } from './logic/transfert';
import { ReferentielHopitaux } from './referentiel.service';
import { TransfertActions } from './transfert-actions.service';
import { Carte, PointCarte } from './ui/carte';
import { Countdown, Frise, StatutTag } from './ui/widgets';

/** Tiroir latéral d'une demande (maquette v3) : carte et trajet, frise, minuteur, historique, actions selon le rôle. */
@Component({
  selector: 'app-transfert-tiroir',
  imports: [A11yModule, DatePipe, RouterLink, Carte, Countdown, Frise, StatutTag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { '(document:keydown.escape)': 'echap()' },
  templateUrl: './transfert-tiroir.html',
  styleUrl: './transfert-tiroir.scss',
})
export class TransfertTiroir {
  private readonly auth = inject(AuthService);
  private readonly actions = inject(TransfertActions);
  private readonly ref = inject(ReferentielHopitaux);
  private readonly dialog = inject(Dialog);

  readonly transfert = input.required<Transfert>();
  readonly fermer = output<void>();
  readonly modifie = output<Transfert>();

  protected readonly enCours = signal(false);
  /** Horloge à la seconde : le bouton « Confirmer » disparaît dès l'expiration de la place retenue. */
  private readonly maintenant = signal(Date.now());
  protected readonly libSpec = LIBELLES_SPECIALITE;
  protected readonly libGrav = LIBELLES_GRAVITE;
  protected readonly libStatut = LIBELLES_STATUT;
  protected readonly libAction = LIBELLES_ACTION;

  protected readonly code = computed(() => codeCourt('TRF', this.transfert().id));
  protected readonly mission = computed(() => codeCourt('MSN', this.transfert().missionId));
  protected readonly hopital = computed(() => (this.transfert().serviceId ? this.ref.parService().get(this.transfert().serviceId!) : undefined));
  protected readonly boutons = computed(() => {
    const t = this.transfert();
    const expire = compteARebours(t.expireAt, this.maintenant())?.expire ?? false;
    return actionsDisponibles(t.statut, this.auth.roles(), expire);
  });
  protected readonly points = computed<PointCarte[]>(() => {
    const t = this.transfert();
    const pts: PointCarte[] = [];
    if (t.latitudeOrigine !== null && t.longitudeOrigine !== null) pts.push({ lat: t.latitudeOrigine, lon: t.longitudeOrigine, type: 'origine', libelle: 'Patient' });
    const h = this.hopital();
    if (h) pts.push({ lat: h.latitude, lon: h.longitude, type: 'choix', libelle: h.nom });
    return pts;
  });

  constructor() {
    this.ref.charger();
    const horloge = setInterval(() => this.maintenant.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(horloge));
  }

  /** Échap ferme le tiroir, sauf si une boîte de confirmation est ouverte par-dessus (elle se ferme seule). */
  protected echap(): void {
    if (!this.dialog.openDialogs.length) this.fermer.emit();
  }

  protected agir(a: ActionTransfert): void {
    this.enCours.set(true);
    this.actions
      .executer(a, this.transfert())
      .pipe(finalize(() => this.enCours.set(false)))
      .subscribe({ next: (t) => this.modifie.emit(t), error: () => {} });
  }
}
