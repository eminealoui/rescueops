import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { LIBELLES_STATUT_AFFECTATION, StatutAffectation } from '../../../core/api/models';

/** Badge coloré pour les statuts d'affectation (PROPOSEE, CONFIRMEE, ANNULEE). */
@Component({
  selector: 'app-statut-affectation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="tag" [class]="'tag ' + ton()">{{ libelle() }}</span>`,
  styles: `
    .tag { display: inline-flex; align-items: center; padding: 3px 8px; border-radius: 999px; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: .4px; }
    .t-ok { background: var(--ok-soft); color: var(--ok); }
    .t-warn { background: var(--warn-soft); color: #9c6500; }
    .t-danger { background: var(--danger-soft); color: var(--danger); }
  `,
})
export class StatutAffectationTag {
  readonly statut = input.required<StatutAffectation>();

  protected readonly ton = computed(() => {
    switch (this.statut()) {
      case 'CONFIRMEE':
        return 't-ok';
      case 'PROPOSEE':
        return 't-warn';
      case 'ANNULEE':
        return 't-danger';
      default:
        return '';
    }
  });

  protected readonly libelle = computed(() => LIBELLES_STATUT_AFFECTATION[this.statut()] ?? this.statut());
}
