import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DispatchApi } from '../../core/api/dispatch.api';
import { AffectationResponse, StatutAffectation, codeCourt } from '../../core/api/models';
import { ToastService } from '../../core/ui/toast.service';
import { StatutAffectationTag } from './ui/dispatch-widgets';

const RAFRAICHISSEMENT_MS = 15_000;

@Component({
  selector: 'app-affectations',
  imports: [DatePipe, RouterLink, StatutAffectationTag],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './affectations.html',
  styleUrl: './affectations.scss',
})
export class Affectations {
  private readonly api = inject(DispatchApi);
  private readonly toast = inject(ToastService);

  /** Identifiant pré-sélectionné (ex: redirection après création ?id=...) */
  readonly id = input<string | undefined>();

  protected readonly filtres: { libelle: string; statut: StatutAffectation | null }[] = [
    { libelle: 'Toutes', statut: null },
    { libelle: 'Proposées', statut: 'PROPOSEE' },
    { libelle: 'Confirmées', statut: 'CONFIRMEE' },
    { libelle: 'Annulées', statut: 'ANNULEE' },
  ];

  protected readonly statutSelectionne = signal<StatutAffectation | null>(null);
  protected readonly recherche = signal('');
  protected readonly chargement = signal(false);
  protected readonly actionEnCours = signal<string | number | null>(null);
  protected readonly affectations = signal<AffectationResponse[]>([]);

  protected readonly listeFiltree = computed(() => {
    const q = this.recherche().trim().toLowerCase();
    const liste = this.affectations();
    if (!q) return liste;
    return liste.filter(
      (a) =>
        String(a.id).toLowerCase().includes(q) ||
        a.incidentId.toLowerCase().includes(q) ||
        a.ressourceId.toLowerCase().includes(q) ||
        a.statut.toLowerCase().includes(q)
    );
  });

  constructor() {
    this.charger();
    const timer = setInterval(() => this.charger(false), RAFRAICHISSEMENT_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }

  protected filtrer(s: StatutAffectation | null): void {
    this.statutSelectionne.set(s);
    this.charger(true);
  }

  protected charger(indicateur = true): void {
    if (indicateur) this.chargement.set(true);
    this.api.listerAffectations(this.statutSelectionne()).subscribe({
      next: (data) => {
        this.affectations.set(data);
        this.chargement.set(false);
      },
      // L'intercepteur affiche le toast d'erreur ; on arrête juste l'indicateur.
      error: () => this.chargement.set(false),
    });
  }

  protected confirmer(aff: AffectationResponse): void {
    this.actionEnCours.set(aff.id);
    this.api.confirmerAffectation(String(aff.id)).subscribe({
      next: (modifiee) => {
        this.toast.ok(`Affectation ${this.code(modifiee.id)} confirmée avec succès.`);
        this.mettreAJourItem(modifiee);
        this.actionEnCours.set(null);
      },
      error: () => this.actionEnCours.set(null),
    });
  }

  /** BF-09 : le backend annule puis propose une alternative ; on recharge donc la liste entière. */
  protected annuler(aff: AffectationResponse): void {
    this.actionEnCours.set(aff.id);
    this.api.annulerAffectation(String(aff.id)).subscribe({
      next: (alternative) => {
        this.toast.ok(
          alternative
            ? `Affectation ${this.code(aff.id)} annulée · nouvelle proposition ${this.code(alternative.id)}.`
            : `Affectation ${this.code(aff.id)} annulée · aucune alternative disponible.`
        );
        this.actionEnCours.set(null);
        this.charger(false);
      },
      error: () => this.actionEnCours.set(null),
    });
  }

  private mettreAJourItem(item: AffectationResponse): void {
    this.affectations.update((liste) => liste.map((a) => (a.id === item.id ? item : a)));
  }

  protected estSelectionnee(idAffectation: number | string): boolean {
    return !!this.id() && String(idAffectation) === this.id();
  }

  protected tonScore(score: number | null): string {
    if (score === null) return 'badge-score';
    if (score >= 70) return 'badge-score haut';
    if (score >= 40) return 'badge-score moyen';
    return 'badge-score bas';
  }

  protected code(id: number | string): string {
    return codeCourt('AFF', id);
  }
}
