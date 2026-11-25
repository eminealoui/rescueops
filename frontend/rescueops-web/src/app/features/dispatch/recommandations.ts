import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { DispatchApi } from '../../core/api/dispatch.api';
import { RecommandationResponse, codeCourt } from '../../core/api/models';
import { ToastService } from '../../core/ui/toast.service';
import { ScoreRing } from '../hospital/ui/widgets';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'app-recommandations',
  imports: [ReactiveFormsModule, ScoreRing],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './recommandations.html',
  styleUrl: './recommandations.scss',
})
export class Recommandations {
  private readonly api = inject(DispatchApi);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  // Valeurs alignées sur le contrat ms-resource / ms-dispatch (AMBULANCE, EQUIPE, EQUIPEMENT).
  protected readonly typesRessource = [
    { label: 'Tous les types', value: '' },
    { label: 'Ambulance', value: 'AMBULANCE' },
    { label: 'Équipe', value: 'EQUIPE' },
    { label: 'Équipement', value: 'EQUIPEMENT' },
  ];

  protected readonly form = new FormGroup({
    incidentId: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(UUID_REGEX)] }),
    latitudeIncident: new FormControl<number>(36.8065, { nonNullable: true }),
    longitudeIncident: new FormControl<number>(10.1815, { nonNullable: true }),
    typeRessource: new FormControl<string>('', { nonNullable: true }),
  });

  protected readonly calculEnCours = signal(false);
  protected readonly affectationEnCours = signal<string | null>(null);
  protected readonly recommandations = signal<RecommandationResponse[] | null>(null);
  protected readonly modeConsulte = signal(false);

  constructor() {
    this.nouvelIncident();
  }

  protected nouvelIncident(): void {
    this.form.controls.incidentId.setValue(crypto.randomUUID());
    this.recommandations.set(null);
    this.modeConsulte.set(false);
  }

  protected calculer(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const val = this.form.getRawValue();
    this.calculEnCours.set(true);
    this.modeConsulte.set(false);

    this.api
      .calculerRecommandations({
        incidentId: val.incidentId,
        latitudeIncident: val.latitudeIncident,
        longitudeIncident: val.longitudeIncident,
        typeRessource: val.typeRessource || null,
      })
      .pipe(finalize(() => this.calculEnCours.set(false)))
      .subscribe({
        next: (res) => {
          this.recommandations.set(res);
          this.toast.ok(`${res.length} recommandation(s) calculée(s) pour cet incident.`);
        },
        // Les erreurs HTTP sont déjà traduites en toast par errorInterceptor (401/403/404/409...).
      });
  }

  protected consulterExistant(): void {
    const id = this.form.controls.incidentId.value.trim();
    if (!id || this.form.controls.incidentId.invalid) {
      this.toast.danger('Veuillez renseigner un UUID d’incident valide.');
      return;
    }
    this.calculEnCours.set(true);
    this.api
      .getRecommandations(id)
      .pipe(finalize(() => this.calculEnCours.set(false)))
      .subscribe({
        next: (res) => {
          this.recommandations.set(res);
          this.modeConsulte.set(true);
          this.toast.ok(`${res.length} recommandation(s) enregistrée(s) trouvée(s).`);
        },
      });
  }

  protected affecter(rec: RecommandationResponse): void {
    const incidentId = this.form.controls.incidentId.value;
    this.affectationEnCours.set(rec.ressourceId);

    this.api
      .creerAffectation({
        incidentId,
        ressourceId: rec.ressourceId,
        typeRessource: rec.typeRessource,
        score: rec.score,
      })
      .pipe(finalize(() => this.affectationEnCours.set(null)))
      .subscribe({
        next: (aff) => {
          this.toast.ok(`Affectation créée avec succès pour la ressource ${this.codeRessource(rec.ressourceId)} !`);
          void this.router.navigate(['/dispatch/affectations'], { queryParams: { id: aff.id } });
        },
      });
  }

  protected codeRessource(id: string): string {
    return codeCourt('RES', id);
  }
}
