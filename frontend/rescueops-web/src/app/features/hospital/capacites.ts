import { ChangeDetectionStrategy, Component, DestroyRef, afterNextRender, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EtatService, LIBELLES_SPECIALITE, SPECIALITES, Specialite, TableauDeBord } from '../../core/api/models';
import { HospitalApi } from '../../core/api/hospital.api';
import { AuthService } from '../../core/auth/auth.service';

const RAFRAICHISSEMENT_MS = 30_000;

/** Capacités hospitalières (maquette v3) : héro Voir / Anticiper / Coordonner / Agir, puis une carte par service. */
@Component({
  selector: 'app-capacites',
  imports: [FormsModule, RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './capacites.html',
  styleUrl: './capacites.scss',
})
export class Capacites {
  private readonly api = inject(HospitalApi);
  protected readonly auth = inject(AuthService);
  protected readonly specialites = SPECIALITES;
  protected readonly libelles = LIBELLES_SPECIALITE;
  protected readonly filtres: { libelle: string; etat: EtatService | null }[] = [
    { libelle: 'Tous', etat: null },
    { libelle: 'Disponibles', etat: 'DISPONIBLE' },
    { libelle: 'Réservés', etat: 'RESERVE' },
    { libelle: 'Saturés', etat: 'SATURE' },
  ];

  protected readonly donnees = signal<TableauDeBord | null>(null);
  protected readonly etat = signal<EtatService | null>(null);
  protected readonly specialite = signal<Specialite | null>(null);
  protected readonly ville = signal('');
  /** Les barres partent de 0 puis s'animent jusqu'à leur valeur. */
  protected readonly anime = signal(false);
  protected readonly maj = signal<Date | null>(null);

  /** Toutes les villes connues (cumulées au fil des chargements) : la liste ne rétrécit pas quand un filtre est actif. */
  private readonly villesConnues = signal<string[]>([]);
  protected readonly villes = this.villesConnues.asReadonly();

  constructor() {
    this.charger();
    const id = setInterval(() => this.charger(), RAFRAICHISSEMENT_MS);
    inject(DestroyRef).onDestroy(() => clearInterval(id));
    afterNextRender(() => setTimeout(() => this.anime.set(true), 120));
  }

  protected choisirEtat(e: EtatService | null): void {
    this.etat.set(e);
    this.charger();
  }

  protected charger(): void {
    this.api.capacites({ etat: this.etat(), specialite: this.specialite(), ville: this.ville() || null }).subscribe((d) => {
      this.donnees.set(d);
      this.maj.set(new Date());
      this.villesConnues.update((v) => [...new Set([...v, ...d.services.map((s) => s.ville)])].sort((a, b) => a.localeCompare(b, 'fr')));
    });
  }

  protected occupation(libres: number, total: number): number {
    return total ? Math.round(((total - libres) / total) * 100) : 100;
  }
}
