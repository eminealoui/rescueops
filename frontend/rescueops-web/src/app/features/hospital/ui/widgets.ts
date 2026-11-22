import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, input, signal } from '@angular/core';
import { LIBELLES_STATUT, StatutTransfert } from '../../../core/api/models';
import { ETAPES_FRISE, compteARebours, etapeFrise, tonStatut } from '../logic/transfert';
import { Transfert } from '../../../core/api/models';

/** Jauge circulaire du score de compatibilité (0 à 100). */
@Component({
  selector: 'app-score-ring',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg viewBox="0 0 46 46" width="46" height="46" aria-hidden="true">
      <circle cx="23" cy="23" r="19" fill="none" stroke="var(--line)" stroke-width="4" />
      <circle cx="23" cy="23" r="19" fill="none" [attr.stroke]="couleur()" stroke-width="4" stroke-linecap="round"
              [attr.stroke-dasharray]="circonference" [attr.stroke-dashoffset]="decalage()" class="arc" />
    </svg>
    <span class="num">{{ valeur() }}</span>
  `,
  host: { role: 'img', '[attr.aria-label]': '"Score de compatibilité : " + valeur() + " sur 100"' },
  styles: `
    :host { width: 46px; height: 46px; position: relative; display: inline-block; flex: none; }
    svg { transform: rotate(-90deg); }
    .arc { transition: stroke-dashoffset 1s var(--ease); }
    .num { position: absolute; inset: 0; display: grid; place-items: center; font-size: 17px; }
  `,
})
export class ScoreRing {
  readonly valeur = input.required<number>();
  protected readonly circonference = 2 * Math.PI * 19;
  protected readonly decalage = computed(() => this.circonference * (1 - Math.max(0, Math.min(100, this.valeur())) / 100));
  protected readonly couleur = computed(() => (this.valeur() >= 75 ? 'var(--ok)' : this.valeur() >= 50 ? 'var(--warn)' : 'var(--danger)'));
}

/** Minuteur avant expiration d'une place retenue (mm:ss, puis « expirée »). */
@Component({
  selector: 'app-countdown',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (etat(); as e) {<span class="num mono" [class.urgent]="e.urgent" [class.expire]="e.expire">{{ e.texte }}</span>}`,
  styles: `
    .num { font-size: inherit; }
    .urgent { color: var(--danger); animation: clign 1s infinite; }
    .expire { color: var(--danger); font-size: .7em; }
    @keyframes clign { 50% { opacity: .5; } }
  `,
})
export class Countdown {
  readonly expireAt = input<string | null>(null);
  private readonly maintenant = signal(Date.now());
  protected readonly etat = computed(() => compteARebours(this.expireAt(), this.maintenant()));

  constructor() {
    const id = setInterval(() => this.maintenant.set(Date.now()), 1000);
    inject(DestroyRef).onDestroy(() => clearInterval(id));
  }
}

/** Étiquette colorée d'un statut de transfert. */
@Component({
  selector: 'app-statut',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="tag" [class]="'tag t-' + ton()">{{ libelle() }}</span>`,
  styles: `
    .t-ok { background: var(--ok-soft); color: var(--ok); }
    .t-warn { background: var(--warn-soft); color: var(--text); }
    .t-warn::before { background: var(--warn); }
    .t-danger { background: var(--danger-soft); color: var(--danger); }
    .t-neutre { background: var(--surface-2); color: var(--text-2); }
  `,
})
export class StatutTag {
  readonly statut = input.required<StatutTransfert>();
  protected readonly ton = computed(() => tonStatut(this.statut()));
  protected readonly libelle = computed(() => LIBELLES_STATUT[this.statut()].toUpperCase());
}

/** Frise des étapes : Demandée → Place retenue → Confirmée → Admis → Sorti. */
@Component({
  selector: 'app-frise',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ol class="frise" [attr.aria-label]="'Avancement : ' + libelles[etapes[frise().active]!]">
      @for (e of etapes; track e; let i = $index) {
        <li [class.fait]="i < frise().active || (i === frise().active && i === etapes.length - 1)"
            [class.maintenant]="i === frise().active && !frise().arret && i < etapes.length - 1"
            [class.arret]="i === frise().active && !!frise().arret" [attr.aria-current]="i === frise().active ? 'step' : null">
          <span class="trait"></span><span class="lib">{{ libelles[e] }}</span>
        </li>
      }
    </ol>
    @if (frise().arret; as a) { <p class="arret-txt">Demande {{ libelles[a].toLowerCase() }}.</p> }
  `,
  styles: `
    .frise { list-style: none; padding: 0; margin: 0; display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; }
    li { display: grid; gap: 6px; }
    .trait { height: 4px; border-radius: 2px; background: var(--line); }
    .lib { font-size: 11px; color: var(--text-3); }
    .fait .trait { background: var(--brand); }
    .maintenant .trait { background: var(--warn); animation: pulse 1.6s infinite; }
    .maintenant .lib, .fait .lib { color: var(--text); }
    .arret .trait { background: var(--danger); }
    .arret-txt { margin-top: 8px; font-size: 12px; color: var(--danger); }
    @keyframes pulse { 50% { opacity: .4; } }
  `,
})
export class Frise {
  readonly transfert = input.required<Pick<Transfert, 'statut' | 'historique'>>();
  protected readonly etapes = ETAPES_FRISE;
  protected readonly libelles = LIBELLES_STATUT;
  protected readonly frise = computed(() => etapeFrise(this.transfert()));
}
