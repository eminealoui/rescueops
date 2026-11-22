import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

/** Pile de toasts en bas de l'écran (pilule bleu pétrole de la maquette). Annoncée aux lecteurs d'écran. */
@Component({
  selector: 'app-toast-host',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="pile" aria-live="polite" role="status">
      @for (t of toasts.toasts(); track t.id) {
        <div class="toast" [class]="'toast toast-' + t.kind">
          <span class="dot" aria-hidden="true"></span>
          <span>{{ t.text }}</span>
          <button type="button" class="x" (click)="toasts.fermer(t.id)" aria-label="Fermer le message">×</button>
        </div>
      }
    </div>
  `,
  styles: `
    /* En haut : en bas, la pile couvrirait les boutons du tiroir latéral et des boîtes de dialogue. */
    .pile { position: fixed; left: 50%; top: 76px; transform: translateX(-50%); display: grid; gap: 8px; z-index: 1100;
            width: max-content; max-width: calc(100vw - 32px); }
    .toast { display: flex; align-items: center; gap: 10px; background: var(--brand-deep); color: #fff; padding: 11px 12px 11px 18px;
             border-radius: var(--radius-pill); box-shadow: var(--shadow-deep); font-size: 13px; animation: in var(--t-slow) var(--ease); }
    .dot { width: 8px; height: 8px; border-radius: 50%; flex: none; background: var(--brand-on); }
    .toast-ok .dot { background: var(--ok); }
    .toast-danger .dot { background: var(--danger); }
    .x { border: 0; background: transparent; color: rgba(255, 255, 255, .7); font-size: 18px; line-height: 1; cursor: pointer; padding: 0 6px; }
    .x:hover { color: #fff; }
    @keyframes in { from { opacity: 0; transform: translateY(-16px); } }
  `,
})
export class ToastHost {
  protected readonly toasts = inject(ToastService);
}
