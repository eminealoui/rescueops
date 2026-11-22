import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { LIBELLES_GRAVITE, LIBELLES_SPECIALITE, StatutTransfert, Transfert, codeCourt } from '../../core/api/models';
import { TransferApi } from '../../core/api/hospital.api';
import { TransfertTiroir } from './transfert-tiroir';
import { Countdown, StatutTag } from './ui/widgets';

const RAFRAICHISSEMENT_MS = 15_000;

/** Liste des demandes de transfert, filtrable par statut. Un clic ouvre le tiroir de traitement (?id=... dans l'URL). */
@Component({
  selector: 'app-transferts',
  imports: [DatePipe, StatutTag, Countdown, TransfertTiroir],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="entete">
      <div>
        <div class="eyebrow">Suivi</div>
        <h1 class="page-title">Demandes de <em>transfert</em></h1>
      </div>
      <div class="chips" role="group" aria-label="Filtrer par statut">
        @for (f of filtres; track f.libelle) {
          <button type="button" class="chip" [class.on]="statut() === f.statut" (click)="filtrer(f.statut)">{{ f.libelle }}</button>
        }
      </div>
    </div>

    <div class="card tableau">
      <div class="scroll">
        <table class="table">
          <thead>
            <tr><th>Demande</th><th>Besoin</th><th>Établissement</th><th>Statut</th><th>Échéance</th><th>Créée</th></tr>
          </thead>
          <tbody>
            @for (t of liste(); track t.id) {
              <tr class="ligne" tabindex="0" (click)="ouvrir(t.id)" (keydown.enter)="ouvrir(t.id)" [class.sel]="t.id === ouvert()?.id">
                <td class="mono">{{ code(t.id) }}</td>
                <td>{{ libSpec[t.specialiteRequise] }} <span class="grav" [class.crit]="t.gravite === 'CRITIQUE'">· {{ libGrav[t.gravite] }}</span></td>
                <td>{{ t.hopitalNom ?? '—' }}</td>
                <td><app-statut [statut]="t.statut" /></td>
                <td class="mono">@if (t.statut === 'PLACE_RETENUE') { <app-countdown [expireAt]="t.expireAt" /> } @else { — }</td>
                <td class="petit">{{ t.createdAt | date: 'dd/MM/yyyy HH:mm' }}</td>
              </tr>
            } @empty {
              <tr><td colspan="6" class="vide">{{ chargement() ? 'Chargement…' : 'Aucune demande.' }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    </div>

    @if (ouvert(); as t) {
      <app-transfert-tiroir [transfert]="t" (fermer)="fermer()" (modifie)="apresAction($event)" />
    }
  `,
  styles: `
    .entete { display: flex; justify-content: space-between; align-items: flex-end; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
    .chips { display: flex; gap: 6px; flex-wrap: wrap; }
    .tableau { padding: 0; overflow: hidden; }
    .tableau:hover { transform: none; }
    .scroll { overflow-x: auto; }
    .ligne { cursor: pointer; }
    .ligne.sel td { background: var(--brand-soft); }
    .grav { color: var(--text-3); font-size: 12px; }
    .grav.crit { color: var(--danger); font-weight: 600; }
    .petit { font-size: 12px; color: var(--text-2); }
    .vide { text-align: center; color: var(--text-3); padding: 32px; }
  `,
})
export class Transferts {
  private readonly api = inject(TransferApi);
  private readonly router = inject(Router);
  /** Demande à ouvrir (?id=...). */
  readonly id = input<string | undefined>();

  protected readonly libSpec = LIBELLES_SPECIALITE;
  protected readonly libGrav = LIBELLES_GRAVITE;
  protected readonly filtres: { libelle: string; statut: StatutTransfert | null }[] = [
    { libelle: 'Toutes', statut: null },
    { libelle: 'Demandées', statut: 'DEMANDEE' },
    { libelle: 'Places retenues', statut: 'PLACE_RETENUE' },
    { libelle: 'Confirmées', statut: 'CONFIRMEE' },
    { libelle: 'Admis', statut: 'ADMIS' },
  ];
  protected readonly statut = signal<StatutTransfert | null>(null);
  protected readonly liste = signal<Transfert[]>([]);
  protected readonly chargement = signal(false);
  protected readonly ouvert = signal<Transfert | null>(null);

  constructor() {
    this.charger();
    const t = setInterval(() => this.charger(), RAFRAICHISSEMENT_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(t));
    effect(() => {
      const id = this.id();
      if (id) this.api.detail(id).subscribe((d) => this.ouvert.set(d));
      else this.ouvert.set(null);
    });
  }

  protected code(id: string): string {
    return codeCourt('TRF', id);
  }

  protected filtrer(s: StatutTransfert | null): void {
    this.statut.set(s);
    this.charger();
  }

  protected ouvrir(id: string): void {
    void this.router.navigate([], { queryParams: { id }, queryParamsHandling: 'merge' });
  }

  protected fermer(): void {
    void this.router.navigate([], { queryParams: { id: null }, queryParamsHandling: 'merge' });
  }

  /** Après une action : détail relu (avec l'historique à jour) et liste rafraîchie. */
  protected apresAction(t: Transfert): void {
    this.api.detail(t.id).subscribe((d) => this.ouvert.set(d));
    this.charger();
  }

  private charger(): void {
    this.chargement.set(true);
    this.api.lister(this.statut()).subscribe({
      next: (l) => {
        this.liste.set(l);
        this.chargement.set(false);
      },
      error: () => this.chargement.set(false),
    });
  }
}
