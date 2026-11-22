import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ThemeService } from '../theme/theme.service';

/** Logo RescueOps : croix médicale traversée par un pouls, balise ambre (source : docs/design/logo.svg). */
@Component({
  selector: 'app-brand',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<img class="logo" src="logo.svg" alt="" width="30" height="30" /><span>RescueOps</span>`,
  styles: `
    :host { display: inline-flex; align-items: center; gap: 10px; font-weight: 600; font-size: 16px; letter-spacing: -.2px; color: var(--text); }
    .logo { width: 30px; height: 30px; border-radius: 8px; flex: none; box-shadow: 0 4px 10px -4px rgba(11, 47, 64, .5); }
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
