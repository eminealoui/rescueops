import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, of, switchMap } from 'rxjs';
import { Candidat, GRAVITES, Gravite, LIBELLES_GRAVITE, LIBELLES_SPECIALITE, SPECIALITES, Specialite, Transfert } from '../../core/api/models';
import { HospitalApi, TransferApi } from '../../core/api/hospital.api';
import { ToastService } from '../../core/ui/toast.service';
import { LiveFeed } from '../../layout/live-feed.service';
import { ReferentielHopitaux } from './referentiel.service';
import { Carte, PointCarte } from './ui/carte';
import { ScoreRing } from './ui/widgets';

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** Nouvelle demande de transfert : besoin du patient → établissements compatibles classés → réservation d'une place. */
@Component({
  selector: 'app-nouvelle-demande',
  imports: [ReactiveFormsModule, Carte, ScoreRing],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './nouvelle-demande.html',
  styleUrl: './nouvelle-demande.scss',
})
export class NouvelleDemande {
  private readonly hopitaux = inject(HospitalApi);
  private readonly transferts = inject(TransferApi);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);
  private readonly feed = inject(LiveFeed);
  protected readonly ref = inject(ReferentielHopitaux);

  /** Pré-remplissage depuis l'écran Capacités (?specialite=...). */
  readonly specialite = input<string | undefined>();

  protected readonly specialites = SPECIALITES;
  protected readonly gravites = GRAVITES;
  protected readonly libSpec = LIBELLES_SPECIALITE;
  protected readonly libGrav = LIBELLES_GRAVITE;

  protected readonly form = new FormGroup({
    missionId: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(UUID)] }),
    specialiteRequise: new FormControl<Specialite>('TRAUMATOLOGIE', { nonNullable: true }),
    gravite: new FormControl<Gravite>('ELEVEE', { nonNullable: true }),
  });
  protected readonly position = signal<{ lat: number; lon: number } | null>(null);

  protected readonly candidats = signal<Candidat[] | null>(null);
  protected readonly choix = signal<string | null>(null);
  protected readonly recherche = signal(false);
  protected readonly reservation = signal(false);
  /** Demande déjà créée (si la réservation a échoué, on réessaie sur la même demande). */
  private readonly creee = signal<Transfert | null>(null);

  protected readonly candidatChoisi = computed(() => this.candidats()?.find((c) => c.serviceId === this.choix()) ?? null);
  protected readonly points = computed<PointCarte[]>(() => {
    const pts: PointCarte[] = [];
    const pos = this.position();
    if (pos) pts.push({ ...pos, type: 'origine', libelle: 'Position du patient' });
    for (const c of this.candidats() ?? []) {
      const h = this.ref.parId().get(c.hopitalId);
      if (h) pts.push({ lat: h.latitude, lon: h.longitude, libelle: `${c.hopitalNom} · score ${c.score}`, type: c.serviceId === this.choix() ? 'choix' : 'hopital' });
    }
    return pts;
  });

  constructor() {
    this.ref.charger();
    this.nouvelleMission();
    effect(() => {
      const s = this.specialite();
      if (s && (SPECIALITES as readonly string[]).includes(s)) this.form.controls.specialiteRequise.setValue(s as Specialite);
    });
    // Changer le besoin invalide les résultats affichés.
    this.form.valueChanges.subscribe(() => this.candidats.set(null));
  }

  /** En attendant le module Interventions (MS4), une mission de démonstration est générée. */
  protected nouvelleMission(): void {
    this.form.controls.missionId.setValue(crypto.randomUUID());
    this.creee.set(null);
  }

  protected placer(p: { lat: number; lon: number }): void {
    this.position.set(p);
    this.candidats.set(null);
  }

  protected chercher(): void {
    if (this.form.invalid) return this.form.markAllAsTouched();
    const v = this.form.getRawValue();
    const pos = this.position();
    this.recherche.set(true);
    this.hopitaux
      .compatibles(v.specialiteRequise, v.gravite, pos?.lat ?? null, pos?.lon ?? null)
      .pipe(finalize(() => this.recherche.set(false)))
      .subscribe((c) => {
        this.candidats.set(c);
        this.choix.set(c[0]?.serviceId ?? null);
      });
  }

  protected reserver(): void {
    const c = this.candidatChoisi();
    if (!c || this.reservation()) return;
    const v = this.form.getRawValue();
    const pos = this.position();
    this.reservation.set(true);
    const demande$ = this.creee()
      ? of(this.creee()!)
      : this.transferts.creer({ ...v, latitudeOrigine: pos?.lat ?? null, longitudeOrigine: pos?.lon ?? null });
    demande$
      .pipe(
        switchMap((t) => {
          this.creee.set(t);
          return this.transferts.reserver(t.id, c.serviceId);
        }),
        finalize(() => this.reservation.set(false)),
      )
      .subscribe({
        next: (t) => {
          this.toast.ok(`Place retenue · ${c.hopitalNom}. Le coordinateur doit confirmer avant l'expiration.`);
          this.feed.rafraichir();
          void this.router.navigate(['/hopitaux/transferts'], { queryParams: { id: t.id } });
        },
        // La place vient d'être prise ailleurs (409) : on relit les compatibles à jour.
        error: () => this.chercher(),
      });
  }
}
