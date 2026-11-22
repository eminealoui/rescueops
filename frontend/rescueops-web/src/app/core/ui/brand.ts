import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ThemeService } from '../theme/theme.service';

/** Logo RescueOps (losange rayé jaune et bleu clair). */
@Component({
  selector: 'app-brand',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="logo" aria-hidden="true"><i></i></span><span>RescueOps</span>`,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 10px; font-weight: 600; font-size: 16px; letter-spacing: -.2px; color: var(--text); }
    .logo { width: 28px; height: 28px; border-radius: 8px; background: var(--brand-deep); display: grid; place-items: center; }
    .logo i { width: 12px; height: 12px; border-radius: 2px; transform: rotate(45deg);
              background: repeating-linear-gradient(135deg, #f2c230 0 3px, var(--brand-on) 3px 6px); }
  `,
})
export class Brand {}

/** Bouton rond clair / sombre. */
@Component({
  selector: 'app-theme-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button type="button" class="theme" (click)="theme.basculer()"
            [attr.aria-label]="theme.theme() === 'dark' ? 'Passer en mode clair' : 'Passer en mode sombre'"
            [title]="theme.theme() === 'dark' ? 'Mode clair' : 'Mode sombre'">
      {{ theme.theme() === 'dark' ? '☀' : '☾' }}
    </button>
  `,
  styles: `
    .theme { width: 36px; height: 36px; border-radius: 50%; border: 1px solid var(--line); background: var(--surface-2); color: var(--text);
             cursor: pointer; display: grid; place-items: center; transition: transform .4s var(--ease); }
    .theme:hover { transform: rotate(25deg); }
  `,
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);
}
