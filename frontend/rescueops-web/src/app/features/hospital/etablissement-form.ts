import { DIALOG_DATA, DialogRef } from '@angular/cdk/dialog';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Hopital, HopitalRequest, LIBELLES_SPECIALITE, SPECIALITES, ServiceHospitalier, Specialite } from '../../core/api/models';
import { Carte, PointCarte } from './ui/carte';

/** Boîte « Hôpital » : création ou modification, position choisie en cliquant sur la carte. */
@Component({
  selector: 'app-hopital-dialog',
  imports: [ReactiveFormsModule, Carte],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="dlg" [formGroup]="form" (ngSubmit)="valider()" novalidate>
      <h2>{{ data ? 'Modifier l\\'établissement' : 'Nouvel établissement' }}</h2>
      <div class="field"><label for="h-nom">Nom</label><input id="h-nom" class="input" formControlName="nom" /></div>
      <div class="deux">
        <div class="field"><label for="h-ville">Ville</label><input id="h-ville" class="input" formControlName="ville" /></div>
        <div class="field"><label for="h-tel">Téléphone</label><input id="h-tel" class="input" formControlName="telephone" placeholder="+216 71 000 000" /></div>
      </div>
      <div class="field"><label for="h-adr">Adresse</label><input id="h-adr" class="input" formControlName="adresse" /></div>
      <div class="field">
        <span class="lbl">Position <span class="mono pos">{{ form.value.latitude }}, {{ form.value.longitude }}</span></span>
        <app-carte class="carte" [points]="point()" [cliquable]="true" (clic)="placer($event)" libelle="Cliquez pour placer l'établissement" />
      </div>
      @if (form.invalid && form.touched) { <span class="field-error">Nom, ville et position sont obligatoires ; téléphone au format +216 71 000 000.</span> }
      <div class="actions">
        <button type="button" class="btn btn-outline" (click)="ref.close()">Annuler</button>
        <button type="submit" class="btn btn-primary">Enregistrer</button>
      </div>
    </form>
  `,
  styles: `
    .dlg { background: var(--surface); color: var(--text); border: 1px solid var(--line); border-radius: var(--radius); padding: 22px;
           box-shadow: var(--shadow-deep); width: min(560px, calc(100vw - 32px)); max-height: calc(100vh - 32px); overflow: auto; display: grid; gap: 12px; }
    h2 { font-size: 18px; font-weight: 600; }
    .deux { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .lbl { font-size: var(--fs-small); font-weight: 600; color: var(--text-2); }
    .pos { font-weight: 400; color: var(--text-3); }
    .carte { height: 220px; border-radius: var(--radius-sm); border: 1px solid var(--line); }
    .actions { display: flex; justify-content: flex-end; gap: 10px; }
    @media (max-width: 480px) { .deux { grid-template-columns: 1fr; } }
  `,
})
export class HopitalDialog {
  protected readonly data = inject<Hopital | null>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<HopitalRequest>>(DialogRef);
  protected readonly form = new FormGroup({
    nom: new FormControl(this.data?.nom ?? '', { nonNullable: true, validators: [Validators.required, Validators.maxLength(150)] }),
    ville: new FormControl(this.data?.ville ?? 'Tunis', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    adresse: new FormControl(this.data?.adresse ?? '', { nonNullable: true, validators: [Validators.maxLength(255)] }),
    telephone: new FormControl(this.data?.telephone ?? '', { nonNullable: true, validators: [Validators.pattern(/^$|^\+?[0-9 ]{8,20}$/)] }),
    latitude: new FormControl(this.data?.latitude ?? 36.8065, { nonNullable: true, validators: [Validators.required, Validators.min(-90), Validators.max(90)] }),
    longitude: new FormControl(this.data?.longitude ?? 10.1815, { nonNullable: true, validators: [Validators.required, Validators.min(-180), Validators.max(180)] }),
  });
  protected readonly point = signal<PointCarte[]>([{ lat: this.form.value.latitude!, lon: this.form.value.longitude!, type: 'choix', libelle: 'Établissement' }]);

  protected placer(p: { lat: number; lon: number }): void {
    this.form.patchValue({ latitude: p.lat, longitude: p.lon });
    this.point.set([{ ...p, type: 'choix', libelle: 'Établissement' }]);
  }

  protected valider(): void {
    if (this.form.invalid) return this.form.markAllAsTouched();
    const v = this.form.getRawValue();
    this.ref.close({ ...v, nom: v.nom.trim(), ville: v.ville.trim(), adresse: v.adresse.trim() || null, telephone: v.telephone.trim() || null });
  }
}

export interface CapaciteSaisie {
  capaciteTotale: number;
  placesDisponibles: number;
  motif: string | null;
}

/** Boîte « Ajuster la capacité » d'un service, avec motif (tracé dans l'historique). */
@Component({
  selector: 'app-capacite-dialog',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="dlg" [formGroup]="form" (ngSubmit)="valider()" novalidate>
      <h2>Ajuster la capacité</h2>
      <p class="aide">Les places retenues ou occupées par des transferts en cours restent réservées : le serveur refuse un ajustement qui les ferait disparaître.</p>
      <div class="deux">
        <div class="field"><label for="c-tot">Capacité totale</label><input id="c-tot" class="input" type="number" min="0" max="2000" formControlName="capaciteTotale" /></div>
        <div class="field"><label for="c-lib">Places libres</label><input id="c-lib" class="input" type="number" min="0" formControlName="placesDisponibles" /></div>
      </div>
      @if (form.errors?.['superieur']) { <span class="field-error">Les places libres ne peuvent pas dépasser la capacité.</span> }
      <div class="field"><label for="c-motif">Motif</label><input id="c-motif" class="input" formControlName="motif" placeholder="Ex. ouverture de 2 lits supplémentaires" /></div>
      <div class="actions">
        <button type="button" class="btn btn-outline" (click)="ref.close()">Annuler</button>
        <button type="submit" class="btn btn-primary">Enregistrer</button>
      </div>
    </form>
  `,
  styles: `
    .dlg { background: var(--surface); color: var(--text); border: 1px solid var(--line); border-radius: var(--radius); padding: 22px;
           box-shadow: var(--shadow-deep); width: min(440px, calc(100vw - 32px)); display: grid; gap: 12px; }
    h2 { font-size: 18px; font-weight: 600; }
    .aide { font-size: 12.5px; color: var(--text-2); }
    .deux { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .actions { display: flex; justify-content: flex-end; gap: 10px; }
  `,
})
export class CapaciteDialog {
  protected readonly data = inject<ServiceHospitalier>(DIALOG_DATA);
  protected readonly ref = inject<DialogRef<CapaciteSaisie>>(DialogRef);
  protected readonly form = new FormGroup(
    {
      capaciteTotale: new FormControl(this.data.capaciteTotale, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(2000)] }),
      placesDisponibles: new FormControl(this.data.placesDisponibles, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
      motif: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    },
    { validators: (g) => (Number(g.get('placesDisponibles')?.value) > Number(g.get('capaciteTotale')?.value) ? { superieur: true } : null) },
  );

  protected valider(): void {
    if (this.form.invalid) return this.form.markAllAsTouched();
    const v = this.form.getRawValue();
    this.ref.close({ capaciteTotale: Number(v.capaciteTotale), placesDisponibles: Number(v.placesDisponibles), motif: v.motif.trim() || null });
  }
}

/** Boîte « Ajouter un service ». */
@Component({
  selector: 'app-service-dialog',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <form class="dlg" [formGroup]="form" (ngSubmit)="valider()" novalidate>
      <h2>Ajouter un service</h2>
      <div class="field">
        <label for="s-spec">Spécialité</label>
        <select id="s-spec" class="select" formControlName="specialite">
          @for (s of specialites; track s) { <option [value]="s">{{ libelles[s] }}</option> }
        </select>
      </div>
      <div class="field"><label for="s-cap">Capacité (toutes les places démarrent libres)</label><input id="s-cap" class="input" type="number" min="0" max="2000" formControlName="capaciteTotale" /></div>
      <div class="actions">
        <button type="button" class="btn btn-outline" (click)="ref.close()">Annuler</button>
        <button type="submit" class="btn btn-primary">Ajouter</button>
      </div>
    </form>
  `,
  styles: `
    .dlg { background: var(--surface); color: var(--text); border: 1px solid var(--line); border-radius: var(--radius); padding: 22px;
           box-shadow: var(--shadow-deep); width: min(400px, calc(100vw - 32px)); display: grid; gap: 12px; }
    h2 { font-size: 18px; font-weight: 600; }
    .actions { display: flex; justify-content: flex-end; gap: 10px; }
  `,
})
export class ServiceDialog {
  protected readonly ref = inject<DialogRef<{ specialite: Specialite; capaciteTotale: number }>>(DialogRef);
  protected readonly specialites = SPECIALITES;
  protected readonly libelles = LIBELLES_SPECIALITE;
  protected readonly form = new FormGroup({
    specialite: new FormControl<Specialite>('URGENCES_GENERALES', { nonNullable: true }),
    capaciteTotale: new FormControl(10, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(2000)] }),
  });

  protected valider(): void {
    if (this.form.invalid) return this.form.markAllAsTouched();
    const v = this.form.getRawValue();
    this.ref.close({ specialite: v.specialite, capaciteTotale: Number(v.capaciteTotale) });
  }
}
