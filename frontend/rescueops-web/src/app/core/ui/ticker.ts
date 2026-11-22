import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export interface TickerItem {
  /** Partie mise en valeur (ex. « TRF-0412 »). */
  code: string;
  texte: string;
}

/** Bandeau EN DIRECT défilant (pause au survol). La liste est dupliquée pour une boucle sans à-coup. */
@Component({
  selector: 'app-ticker',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="util">
      <div class="live"><span class="pulse" aria-hidden="true"></span>EN DIRECT</div>
      <div class="tw">
        @if (items().length) {
          <div class="ticker" [style.animation-duration.s]="duree()">
            @for (it of boucle(); track $index) {
              <span [attr.aria-hidden]="$index >= items().length"><b>{{ it.code }}</b> {{ it.texte }}</span>
            }
          </div>
        } @else {
          <span class="vide">{{ vide() }}</span>
        }
      </div>
      @if (note()) {
        <div class="note mono">{{ note() }}</div>
      }
    </div>
  `,
  styles: `
    .util { height: 34px; background: var(--brand-deep); color: rgba(255, 255, 255, .78); display: flex; align-items: center; font-size: 12px; overflow: hidden; }
    .live { display: flex; align-items: center; gap: 8px; padding: 0 16px; color: #fff; font-weight: 500; height: 100%;
            border-right: 1px solid rgba(255, 255, 255, .12); white-space: nowrap; }
    .pulse { width: 8px; height: 8px; border-radius: 50%; background: #4ade80; box-shadow: 0 0 10px #4ade80; animation: pulse 1.8s infinite; }
    @keyframes pulse { 50% { opacity: .35; } }
    .tw { flex: 1; min-width: 0; overflow: hidden; padding-left: 16px;
          mask-image: linear-gradient(90deg, transparent, #000 4%, #000 96%, transparent); }
    .ticker { display: flex; gap: 42px; width: max-content; animation: tick 45s linear infinite; font-family: var(--font-mono); }
    .ticker:hover { animation-play-state: paused; }
    .ticker b { color: var(--brand-on); font-weight: 500; }
    @keyframes tick { to { transform: translateX(-50%); } }
    .vide { font-family: var(--font-mono); color: rgba(255, 255, 255, .55); }
    .note { padding: 0 16px; color: rgba(255, 255, 255, .5); white-space: nowrap; }
    @media (max-width: 560px) { .note { display: none; } }
  `,
})
export class Ticker {
  readonly items = input<TickerItem[]>([]);
  readonly note = input<string>('');
  readonly vide = input<string>('Aucune activité récente');
  protected readonly boucle = computed(() => [...this.items(), ...this.items()]);
  /** Vitesse constante quelle que soit la longueur de la liste. */
  protected readonly duree = computed(() => Math.max(20, this.items().length * 7));
}
