import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs';
import { DispatchApi } from '../../core/api/dispatch.api';
import { ToastService } from '../../core/ui/toast.service';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'app-nouvelle-affectation',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './nouvelle-affectation.html',
  styleUrl: './nouvelle-affectation.scss',
})
export class NouvelleAffectation {
  private readonly api = inject(DispatchApi);
  private readonly toast = inject(ToastService);
  private readonly router = inject(Router);

  protected readonly form = new FormGroup({
    incidentId: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(UUID_REGEX)] }),
    ressourceId: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.pattern(UUID_REGEX)] }),
    score: new FormControl<number>(85, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(100)] }),
  });

  protected readonly envoi = signal(false);

  constructor() {
    this.genererIncident();
    this.genererRessource();
  }

  protected genererIncident(): void {
    this.form.controls.incidentId.setValue(crypto.randomUUID());
  }

  protected genererRessource(): void {
    this.form.controls.ressourceId.setValue(crypto.randomUUID());
  }

  protected soumettre(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const val = this.form.getRawValue();
    this.envoi.set(true);

    this.api
      .creerAffectation({
        incidentId: val.incidentId,
        ressourceId: val.ressourceId,
        score: val.score,
        typeRessource: null,
      })
      .pipe(finalize(() => this.envoi.set(false)))
      .subscribe({
        next: (aff) => {
          this.toast.ok('Affectation créée avec succès avec le statut PROPOSÉE !');
          void this.router.navigate(['/dispatch/affectations'], { queryParams: { id: aff.id } });
        },
      });
  }
}
