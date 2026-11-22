import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { LIBELLES_GRAVITE, LIBELLES_SPECIALITE, Transfert, codeCourt } from '../../core/api/models';
import { TransferApi } from '../../core/api/hospital.api';
import { ActionTransfert } from './logic/transfert';
import { TransfertActions } from './transfert-actions.service';

/** Admissions (coordinateur) : patients attendus (CONFIRMEE) à admettre, patients présents (ADMIS) à faire sortir. */
@Component({
  selector: 'app-admissions',
  imports: [DatePipe, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="eyebrow">Accueil des patients</div>
    <h1 class="page-title"><em>Admissions</em> et sorties</h1>
    <p class="page-sub">Admettez les patients attendus, puis déclarez leur sortie : la place est aussitôt rendue au service.</p>

    <div class="colonnes">
      @for (col of colonnes; track col.statut) {
        <section class="col">
          <h2>{{ col.titre }} <span class="nb num">{{ (col.statut === 'CONFIRMEE' ? attendus() : presents()).length }}</span></h2>
          @for (t of col.statut === 'CONFIRMEE' ? attendus() : presents(); track t.id) {
            <article class="card patient">
              <div class="haut">
                <a class="mono code" [routerLink]="['../transferts']" [queryParams]="{ id: t.id }">{{ code(t.id) }}</a>
                <span class="grav" [class.crit]="t.gravite === 'CRITIQUE'">{{ libGrav[t.gravite] }}</span>
              </div>
              <div class="hop">{{ t.hopitalNom }}</div>
              <div class="meta">{{ libSpec[t.specialiteRequise] }} · demandé le {{ t.createdAt | date: 'dd/MM/yyyy HH:mm' }}</div>
              <button type="button" class="btn btn-primary" [disabled]="enCours() === t.id" (click)="agir(col.action, t)">{{ col.bouton }}</button>
            </article>
          } @empty {
            <div class="vide">{{ col.vide }}</div>
          }
        </section>
      }
    </div>
  `,
  styles: `
    .colonnes { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; margin-top: 22px; }
    .col { display: grid; gap: 10px; align-content: start; background: var(--surface-2); border: 1px solid var(--line); border-radius: var(--radius); padding: 14px; }
    h2 { font-size: 15px; font-weight: 600; display: flex; align-items: center; gap: 8px; }
    .nb { background: var(--brand-deep); color: #fff; border-radius: var(--radius-pill); padding: 0 9px; font-size: 15px; }
    .patient { display: grid; gap: 4px; }
    .haut { display: flex; justify-content: space-between; align-items: center; }
    .code { color: var(--brand); font-weight: 500; }
    .grav { font-size: 12px; color: var(--text-3); }
    .grav.crit { color: var(--danger); font-weight: 600; }
    .hop { font-weight: 600; }
    .meta { font-size: 12px; color: var(--text-2); }
    .patient .btn { justify-self: end; margin-top: 6px; }
    .vide { color: var(--text-3); text-align: center; padding: 24px 8px; font-size: 13px; }
    @media (max-width: 760px) { .colonnes { grid-template-columns: 1fr; } }
  `,
})
export class Admissions {
  private readonly api = inject(TransferApi);
  private readonly actions = inject(TransfertActions);
  protected readonly libSpec = LIBELLES_SPECIALITE;
  protected readonly libGrav = LIBELLES_GRAVITE;
  protected readonly colonnes = [
    { titre: 'Patients attendus', statut: 'CONFIRMEE', action: 'admettre' as ActionTransfert, bouton: 'Admettre', vide: 'Aucun patient attendu.' },
    { titre: 'Patients admis', statut: 'ADMIS', action: 'sortie' as ActionTransfert, bouton: 'Déclarer la sortie', vide: 'Aucun patient admis.' },
  ];
  protected readonly attendus = signal<Transfert[]>([]);
  protected readonly presents = signal<Transfert[]>([]);
  protected readonly enCours = signal<string | null>(null);

  constructor() {
    this.charger();
    const t = setInterval(() => this.charger(), 15_000);
    inject(DestroyRef).onDestroy(() => clearInterval(t));
  }

  protected code(id: string): string {
    return codeCourt('TRF', id);
  }

  protected agir(a: ActionTransfert, t: Transfert): void {
    this.enCours.set(t.id);
    this.actions.executer(a, t).subscribe({
      next: () => this.charger(),
      error: () => {
        this.enCours.set(null);
        this.charger();
      },
      complete: () => this.enCours.set(null),
    });
  }

  private charger(): void {
    forkJoin([this.api.lister('CONFIRMEE', 0, 100), this.api.lister('ADMIS', 0, 100)]).subscribe(([c, a]) => {
      this.attendus.set(c);
      this.presents.set(a);
    });
  }
}
